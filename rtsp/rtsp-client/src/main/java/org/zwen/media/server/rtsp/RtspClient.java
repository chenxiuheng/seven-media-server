package org.zwen.media.server.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.rtsp.RtspHeaders;
import org.jboss.netty.handler.codec.rtsp.RtspMethods;
import org.jboss.netty.handler.codec.rtsp.RtspVersions;
import org.jboss.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionDataListener;
import com.biasedbit.efflux.session.SingleParticipantSession;

public class RtspClient implements Closeable {
	private static final Logger logger = LoggerFactory
			.getLogger(RtspClient.class);

	private String url;
	private String user;
	private String pass;
	private RtspClientStack stack;
	private List<MediaDescription> mediaDescriptions;
	private List<RtpSession> sessions = new ArrayList<RtpSession>();

	public RtspClient(String url, String user, String pass) {
		this.url = url;
		this.user = user;
		this.pass = pass;

		Pattern pattern = Pattern.compile("^rtsp://([^:/]+)(:([0-9]+))?");
		Matcher m = pattern.matcher(url);
		if (!m.find()) {
			throw new IllegalArgumentException("非法的 RTSP 地址[" + url + "]");
		}
		String host = m.group(1);
		int port = 554;
		try {
			port = Integer.parseInt(m.group(3));
		} catch (Exception e) {
		}

		stack = new RtspClientStack(host, port);
	}

	@SuppressWarnings("unchecked")
	public void connect() throws SdpException {
		stack.connect();
		
		HttpResponse resp = null;

		resp = option(null, null);
		if (resp.getStatus().getCode() == 401) {
			resp = option(user, pass);
		}

		SessionDescription sessionDescription = describe();
		mediaDescriptions = sessionDescription.getMediaDescriptions(false);
		assertNotNull(mediaDescriptions);

	}

	public void start(RtpSessionDataListener listener) throws SdpParseException, NoPortAvailableException {
		for (MediaDescription mediaDescription : mediaDescriptions) {
			RtpSession session = setup(mediaDescription);
			sessions.add(session);
			
			session.addDataListener(listener);

			session.init();
		}

		play();
	}
	
	private void assertNotNull(List<MediaDescription> mediaDescriptions) {
		if (null == mediaDescriptions) {
			throw new ChannelException("MediaDescription Not Found");
		}
	}

	private HttpResponse option(String user, String pass) {
		DefaultHttpRequest request = makeRequest(RtspMethods.OPTIONS);

		if (null != user) {
			String authValue = getAuthValue(user, pass);
			request.headers().add(RtspHeaders.Names.AUTHORIZATION, authValue);
		}

		return stack.send(request).get();
	}

	private SessionDescription describe() {
		DefaultHttpRequest request = makeRequest(RtspMethods.DESCRIBE);
		request.headers().add(RtspHeaders.Names.ACCEPT, "application/sdp");

		HttpResponse resp = stack.send(request).get();
		ChannelBuffer data = resp.getContent();
		byte[] array = new byte[data.readableBytes()];
		data.readBytes(array);
		String sdp = new String(array);

		SessionDescriptionImpl sd = new SessionDescriptionImpl();
		StringTokenizer tokenizer = new StringTokenizer(sdp);
		while (tokenizer.hasMoreChars()) {
			String line = tokenizer.nextToken();

			try {
				SDPParser paser = ParserFactory.createParser(line);
				if (null != paser) {
					SDPField obj = paser.parse();
					sd.addField(obj);
				}
			} catch (ParseException e) {
				logger.warn("fail parse [{}]", line, e);
			}
		}

		return sd;
	}

	private RtpSession setup(MediaDescription md)
			throws NoPortAvailableException, SdpParseException {
		String url;
		if (this.url.endsWith("/")) {
			url = this.url + md.getAttribute("control");
		} else {
			url = this.url + "/" + md.getAttribute("control");
		}
		DefaultHttpRequest request = new DefaultHttpRequest(
				RtspVersions.RTSP_1_0, RtspMethods.SETUP, url);

		int[] ports = PortManager.findAvailablePorts(2);
		request.headers().add(
				RtspHeaders.Names.TRANSPORT,
				RtspHeaders.Values.RTP + "/" + RtspHeaders.Values.AVP + ";"
						+ RtspHeaders.Values.UNICAST + ";"
						+ RtspHeaders.Values.CLIENT_PORT + "=" + ports[0] + "-"
						+ ports[1]);

		HttpResponse resp = stack.send(request).get();
		HttpHeaders headers = resp.headers();

		String transport = headers.get(RtspHeaders.Names.TRANSPORT);
		if (null == transport) {
			throw new IllegalArgumentException("Error Transport");
		}

		String[] attr = StringUtil.split(transport, ';');
		String[] clientPorts = null;
		String[] serverPorts = null;
		String ssrc = null;
		for (int i = 0; i < attr.length; i++) {
			if (attr[i].startsWith(RtspHeaders.Values.CLIENT_PORT)) {
				clientPorts = attr[i].substring(
						RtspHeaders.Values.CLIENT_PORT.length() + 1).split("-");
			} else if (attr[i].startsWith(RtspHeaders.Values.SERVER_PORT)) {
				serverPorts = attr[i].substring(
						RtspHeaders.Values.SERVER_PORT.length() + 1).split("-");
			} else if (attr[i].startsWith(RtspHeaders.Values.SSRC)) {
				ssrc = attr[i].substring(RtspHeaders.Values.SERVER_PORT
						.length() + 1);
			}
		}

		int payloadType = getPayloadType(md);
		RtpParticipant localParticipant = RtpParticipant.createReceiver(
				"0.0.0.0", Integer.valueOf(clientPorts[0]), Integer
						.valueOf(clientPorts[1]));
		RtpParticipant remoteParticipant = RtpParticipant.createReceiver(stack
				.getHost(), Integer.valueOf(serverPorts[0]), Integer
				.valueOf(serverPorts[1]));

		if (null != ssrc) {
			localParticipant.getInfo().setSsrc(Integer.valueOf(ssrc, 16));
		}

		RtpSession rtp = new SingleParticipantSession(stack.getSessionId(),
				payloadType, localParticipant, remoteParticipant);

		return rtp;
	}

	private int getPayloadType(MediaDescription md) {
		String rtpmap;
		try {
			rtpmap = md.getAttribute("rtpmap");

			if (null == rtpmap) {
				return 0;
			} else {
				String strValue = StringUtil.split(rtpmap, ' ')[0];
				return Integer.valueOf(strValue);
			}
		} catch (SdpParseException e) {
			logger.error(e.getMessage(), e);
		}

		return 0;

	}

	private HttpResponse play() {
		HttpRequest request = makeRequest(RtspMethods.PLAY);
		request.headers().set(RtspHeaders.Names.RANGE, "npt=0.000-");

		return stack.send(request).get();
	}

	private DefaultHttpRequest makeRequest(HttpMethod method) {
		return new DefaultHttpRequest(RtspVersions.RTSP_1_0, method, url);
	}

	private String getAuthValue(String user, String pass) {
		byte[] auth = Base64.encodeBase64(new String(user + ":"
				+ (pass != null ? pass : "")).getBytes());
		String authValue = "Basic " + new String(auth);
		return authValue;
	}

	@Override
	public void close() throws IOException {
		try {
			if (null == stack) {
				stack.close();
			}
		} finally {
			for (RtpSession session : sessions) {
				try {
					session.terminate();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

	}
}
