package org.zwen.media.protocol.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
import org.zwen.media.AVStreamDispatcher;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.URLUtils;
import org.zwen.media.protocol.rtsp.sdp.video.h264.H264Receiver;

import com.biasedbit.efflux.participant.RtpParticipant;

public class RtspClient extends AVStreamDispatcher implements Closeable {
	private static final RtpReceiver[] AVSTREMS_EMPTY = new RtpReceiver[0];

	private static final Logger logger = LoggerFactory
			.getLogger(RtspClient.class);

	private String url;
	private String user;
	private String pass;
	private RtspClientStack stack;
	private boolean supportGetParameters = false;

	private Timer timer;
	private RtpReceiver[] avstreams = AVSTREMS_EMPTY;

	public RtspClient(String url, String user, String pass) {
		this.timer = new Timer(true);
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

	public void connect() throws ConnectException {
		stack.connect();

		HttpResponse resp = null;

		resp = option(null, null);
		if (resp.getStatus().getCode() == 401) {
			resp = option(user, pass);
		}

		if (resp.getStatus().getCode() != 200) {
			throw new ConnectException("Fail connect [" + url + "],  "
					+ resp.getStatus());
		}
		
		String options = resp.headers().get(RtspHeaders.Names.PUBLIC);
		if (StringUtils.contains(options, "GET_PARAMETER")) {
			supportGetParameters = true;
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

	@SuppressWarnings("unchecked")
	public void start() throws IOException {
		try {
			// describe
			SessionDescription sessionDescription = describe();
			Vector<MediaDescription> mediaDescriptions = sessionDescription
					.getMediaDescriptions(false);
			assertNotNull(mediaDescriptions);

			AtomicLong syncClock = new AtomicLong(700);
			List<RtpReceiver> streams = new ArrayList<RtpReceiver>();
			Iterator<MediaDescription> iter = (Iterator<MediaDescription>) mediaDescriptions
					.iterator();
			while (iter.hasNext()) {
				MediaDescription ms = iter.next();

				String proto = ms.getMedia().getProtocol();
				String mediaType = ms.getMedia().getMediaType();
				
				boolean isUdp = "RTP/AVP".equalsIgnoreCase(proto) || "RTP/AVP/UDP".equalsIgnoreCase(proto);
				if (!isUdp) {
					throw new UnsupportedOperationException("unsupported proto [" + proto + "]");
				}
				Vector formats = ms.getMedia().getMediaFormats(false);
				if (null == formats) {
					continue;
				}

				RtpReceiver stream = null;
				for (Object item : formats) {
					stream = null;
					String format = (String)item;
					String rtpmap = ms.getAttribute("rtpmap");
					String fmtp = ms.getAttribute("fmtp");
					
					if (null == rtpmap || !rtpmap.startsWith(format)) {
						logger.warn("rtpmap is NULL for {}", ms.getMedia());
						continue;
					}
					
					Matcher rtpMapParams = Pattern.compile(
							"(\\d+) ([^/]+)(/(\\d+)(/([^/]+))?)?(.*)?")
							.matcher(rtpmap);
					if (!rtpMapParams.matches()) {
						logger.warn("{} is NOT legal rtpmap", rtpmap);
						continue;
					}
					
					String encoding = rtpMapParams.group(2);
					String clockRate = rtpMapParams.group(4);
					if ("H264".equals(encoding)) {
						stream = new H264Receiver(syncClock, ms);
					} else if ("audio".equalsIgnoreCase(mediaType)){
						stream = new RtpReceiver(syncClock,new AudioFormat(encoding), ms, null);
					} else if ("video".equalsIgnoreCase(mediaType)) {
						stream = new RtpReceiver(syncClock,new VideoFormat(encoding), ms, null);
					}

					if (null != stream && null != clockRate) {
						AVTimeUnit unit = new AVTimeUnit(1, Integer.valueOf(clockRate));
						stream.setTimeUnit(unit);
					}
				}

				if (null == stream) {
					logger.error("unsupported[{}]", ms.getMedia());
				} else  {
					stream.setStreamIndex(streams.size());
					streams.add(stream);
				}
			}
			this.avstreams = streams.toArray(AVSTREMS_EMPTY);

			// setup
			for (int i = 0; i < avstreams.length; i++) {
				RtpReceiver stream = avstreams[i];
				boolean connect = setup(stream);
				if (!connect) {
					logger.warn("{} Connect Fail", stream);
				}
			}
			fireSetup(avstreams);

			// play
			play();
		} catch (SdpException e) {
			throw new IOException(e.getMessage(), e);
		} catch (NoPortAvailableException e) {
			throw new ConnectException(e.getClass().getSimpleName());
		}
	}

	private void assertNotNull(List<MediaDescription> mediaDescriptions) {
		if (null == mediaDescriptions) {
			throw new ChannelException("MediaDescription Not Found");
		}
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

	
	private boolean setup(RtpReceiver stream)
			throws NoPortAvailableException, SdpParseException {
		MediaDescription md = stream.getMediaDescription();

		final String controlUrl;
		String control = md.getAttribute("control");
		controlUrl = URLUtils.concat(this.url, control);

		DefaultHttpRequest request = new DefaultHttpRequest(
				RtspVersions.RTSP_1_0, RtspMethods.SETUP, controlUrl);

		int[] ports = PortManager.findAvailablePorts(2);
		request.headers().add(
				RtspHeaders.Names.TRANSPORT,
				RtspHeaders.Values.RTP + "/" + RtspHeaders.Values.AVP + ";"
						+ RtspHeaders.Values.UNICAST + ";"
						+ RtspHeaders.Values.CLIENT_PORT + "=" + ports[0] + "-"
						+ ports[1]);

		HttpResponse resp = stack.send(request).get();
		HttpHeaders headers = resp.headers();

		RtpParticipant localParticipant = null;
		RtpParticipant remoteParticipant = null;

		// receiver
		String recvHost = "0.0.0.0";
		Integer recvRTPPort = Integer.valueOf(ports[0]);
		Integer recvRTCPPort = Integer.valueOf(ports[1]);
		localParticipant = RtpParticipant.createReceiver(recvHost, recvRTPPort,
				recvRTCPPort);

		String transport = headers.get(RtspHeaders.Names.TRANSPORT);
		if (null != transport) {
			String[] attr = StringUtil.split(transport, ';');
			String[] serverPorts = null;
			String ssrc = null;
			for (int i = 0; i < attr.length; i++) {
				if (attr[i].startsWith(RtspHeaders.Values.SERVER_PORT)) {
					serverPorts = attr[i].substring(
							RtspHeaders.Values.SERVER_PORT.length() + 1).split(
							"-");
				} else if (attr[i].startsWith(RtspHeaders.Values.SSRC)) {
					ssrc = attr[i].substring(RtspHeaders.Values.SERVER_PORT
							.length() + 1);
				}
			}

			// sender
			String sndHost = stack.getHost();
			Integer sndRTPPort = Integer.valueOf(serverPorts[0]);
			Integer sndRTCPPort = Integer.valueOf(serverPorts[1]);
			remoteParticipant = RtpParticipant.createReceiver(sndHost,
					sndRTPPort, sndRTCPPort);

			if (null != ssrc) {
				localParticipant.getInfo().setSsrc(Integer.valueOf(ssrc, 16));
			}
		}

		String sessionId = headers.get(RtspHeaders.Names.SESSION);
		if (null != sessionId && supportGetParameters) {
			Matcher matcher = Pattern.compile("([^;]+)(.*(timeout=([\\d]+)).*)?").matcher(sessionId);
			if (matcher.matches()) {
				String timeout = matcher.group(4);
				if (null != timeout) {
					int delay = 1000 * 10;
					int period = 1000 * Integer.valueOf(timeout);
					timer.schedule(new java.util.TimerTask() {
						
						@Override
						public void run() {
							getParameters(controlUrl);
							
						}
					}, delay, period);
				}
			}
		}
		
		
		return stream.connect(stack.getSessionId(), localParticipant,
				remoteParticipant, this);
	}

	private HttpResponse play() {
		HttpRequest request = makeRequest(RtspMethods.PLAY);
		request.headers().set(RtspHeaders.Names.RANGE, "npt=0.000-");

		return stack.send(request).get();
	}
	
	private void tearDown() {
		HttpRequest request = makeRequest(RtspMethods.TEARDOWN);

		stack.send(request).get();
	}
	private void getParameters(String controlUrl) {
		DefaultHttpRequest request = new DefaultHttpRequest(
				RtspVersions.RTSP_1_0, RtspMethods.GET_PARAMETER, controlUrl);

		HttpResponse response = stack.send(request).get();
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
			timer.cancel();

			if (null != stack && !stack.isClosed()) {
				tearDown();
				
				stack.close();
			}
		} finally {

			
			for (RtpReceiver stream : avstreams) {
				if (null == stream.getSession()) {
					continue;
				}

				try {
					stream.getSession().terminate();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

		fireClosed();
	}

}
