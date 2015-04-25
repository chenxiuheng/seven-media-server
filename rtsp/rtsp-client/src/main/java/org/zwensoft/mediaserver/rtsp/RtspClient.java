package org.zwensoft.mediaserver.rtsp;

import gov.nist.core.Separators;
import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
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
import org.zwensoft.mediaserver.NoPortAvailableException;
import org.zwensoft.mediaserver.PortManager;

import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.SingleParticipantSession;

public class RtspClient {
	final private static Logger logger = LoggerFactory
			.getLogger(RtspClient.class);
	final private static ExecutorService threadPool = Executors
			.newCachedThreadPool();
	final private String host;
	final private int port;
	final private String url;

	private boolean usedAuth = false;
	final private String user;
	final private String pass;

	private HttpMethod preMethod = null;
	private Channel channel;
	private ClientBootstrap bootstrap;
	private AtomicInteger cseq = new AtomicInteger(1);
	private RtspSession session = new RtspSession();

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
		String port = m.group(3);

		this.host = host;
		this.port = null != port && port.length() > 0 ? Integer.parseInt(port)
				: 554;
	}

	public void start() {
		sendOptions(null, null);
	}

	public ChannelFuture connect() {
		InetSocketAddress address = new InetSocketAddress(host, port);
		bootstrap = getBootstrap(threadPool);
		final ChannelFuture future = bootstrap.connect(address);
		future.awaitUninterruptibly();
		if (!future.isSuccess()) {
			// future.getCause().printStackTrace();
			logger.error("error creating client connection: {}", future
					.getCause().getMessage());
		}

		channel = future.getChannel();

		// future.getChannel().getCloseFuture().awaitUninterruptibly();
		// bootstrap.getFactory().releaseExternalResources();

		return channel.getCloseFuture();
	}

	public void close() {
		try {
			channel.close();
		} finally {
			bootstrap.getFactory().releaseExternalResources();
		}

	}

	private ClientBootstrap getBootstrap(final Executor executor) {
		final ChannelFactory factory = new NioClientSocketChannelFactory(
				executor, executor);
		final ClientBootstrap bootstrap = new ClientBootstrap(factory);
		bootstrap.setPipelineFactory(new ClientPipelineFactory(this));
		bootstrap.setOption("tcpNoDelay", Boolean.TRUE);
		bootstrap.setOption("keepAlive", Boolean.TRUE);
		return bootstrap;
	}

	private void send(HttpRequest request) {
		request.headers().add(RtspHeaders.Names.CSEQ, cseq.getAndIncrement());
		if (null != session && null != session.getSessionId()) {
			request.headers().add(RtspHeaders.Names.SESSION, session.getSessionId());
		}

		channel.write(request);
		preMethod = request.getMethod();

		if (logger.isInfoEnabled()) {
			logger.info(request.getMethod() + " {}\r\n{}\r\n",
					request.getUri(), toString(request.headers()));
		}
	}

	private DefaultHttpRequest makeRequest(HttpMethod method) {
		return new DefaultHttpRequest(RtspVersions.RTSP_1_0, method, url);
	}

	private void sendOptions(String usr, String pass) {
		DefaultHttpRequest request = makeRequest(RtspMethods.OPTIONS);

		if (null != usr) {
			String authValue = getAuthValue(pass);
			request.headers().add(RtspHeaders.Names.AUTHORIZATION, authValue);
		}

		send(request);
	}

	private String getAuthValue(String pass) {
		byte[] auth = Base64.encodeBase64(new String(user + ":"
				+ (pass != null ? pass : "")).getBytes());
		String authValue = "Basic " + new String(auth);
		return authValue;
	}

	private void sendDescribe() {
		DefaultHttpRequest request = makeRequest(RtspMethods.DESCRIBE);
		request.headers().add(RtspHeaders.Names.ACCEPT, "application/sdp");

		send(request);
	}

	private AtomicInteger portsStart = new AtomicInteger(11024);
	private void sendSetup(int trackIndex) {
		try {

			MediaDescription md = session.getMediaDescription(trackIndex);
			String url;
			if (this.url.endsWith("/")) {
				url = this.url + md.getAttribute("control");
			} else {
				url = this.url + "/" + md.getAttribute("control");
			}
			DefaultHttpRequest request = new DefaultHttpRequest(
					RtspVersions.RTSP_1_0, RtspMethods.SETUP, url);

			int[] ports = PortManager.findAvailablePorts(2, portsStart.getAndAdd(2));
			request.headers().add(
					RtspHeaders.Names.TRANSPORT,
					RtspHeaders.Values.RTP + "/" + RtspHeaders.Values.AVP + ";"
							+ RtspHeaders.Values.UNICAST + ";"
							+ RtspHeaders.Values.CLIENT_PORT + "=" + ports[0]
							+ "-" + ports[1]);
			send(request);

		} catch (NoPortAvailableException e) {
			throw new RuntimeException(e);
		} catch (SdpParseException e) {
			throw new RuntimeException(e);
		}
	}

	private void sendPlay() {
		HttpRequest request = makeRequest(RtspMethods.PLAY);

		if (null != user) {
			String authValue = getAuthValue(pass);
			request.headers().set(RtspHeaders.Names.AUTHORIZATION, authValue);
		}
		request.headers().set(RtspHeaders.Names.RANGE, "npt=0.000-");

		send(request);
	}

	public void onResponse(HttpResponse resp) {
		HttpHeaders headers = resp.headers();
		String content = "";
		if (resp.getContent().readableBytes() > 0) {
			byte[] bytes = new byte[resp.getContent().readableBytes()];
			resp.getContent().readBytes(bytes);
			content = new String(bytes);
		}
		
		String sessionId = headers.get(RtspHeaders.Names.SESSION);
		if (null != sessionId) {
			session.setSessionId(sessionId);
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("{}\r\n{}", toString(headers), content);
		}

		int code = resp.getStatus().getCode();
		switch (code) {
		case 200:
			if (RtspMethods.OPTIONS.equals(preMethod)) {
				sendDescribe();
			} else if (RtspMethods.DESCRIBE.equals(preMethod)) {
				onDescribe(headers, content);
			} else if (RtspMethods.SETUP.equals(preMethod)) {
				onSetup(headers, content);
			} else if (RtspMethods.PLAY.equals(preMethod)) {

			} else if (RtspMethods.PAUSE.equals(preMethod)) {

			} else if (RtspMethods.GET_PARAMETER.equals(preMethod)) {

			} else if (RtspMethods.TEARDOWN.equals(preMethod)) {

			}
			break;
		case 401:
			if (!usedAuth) {
				sendOptions(user, pass);
				usedAuth = true;
			} else {
				close(); // avoid to auth endless loop
			}
			break;
		default:
			break;
		}
	}

	private void onDescribe(HttpHeaders headers, String sdp) {
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

		logger.debug("accept sdp {}", sdp);
		this.session.setSessionDescription(sd);

		// setup stream1
		sendSetup(0);
	}

	private void onSetup(HttpHeaders header, String content) {
		String transport = header.get(RtspHeaders.Names.TRANSPORT);
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
				ssrc = attr[i].substring(
						RtspHeaders.Values.SERVER_PORT.length() + 1);
			}
		}

		String id = header.get(RtspHeaders.Names.SESSION);
		int payloadType = getPayloadType(session
				.getMediaDescription(session.getNumRtpSessions()));
		RtpParticipant localParticipant = RtpParticipant.createReceiver(
				"0.0.0.0", Integer.valueOf(clientPorts[0]), Integer
						.valueOf(clientPorts[1]));
		RtpParticipant remoteParticipant = RtpParticipant.createReceiver(host,
				Integer.valueOf(serverPorts[0]), Integer
						.valueOf(serverPorts[1]));
		
		if (null != ssrc) {
			localParticipant.getInfo().setSsrc(Integer.valueOf(ssrc, 16));
		}
		
		RtpSession rtp = new SingleParticipantSession(id, payloadType,
				localParticipant, remoteParticipant);
		
		session.addRtpSession(rtp);

		if (session.getNumRtpSessions() < session.getNumMediaDescriptions()) {
			int nextMediaIndex = session.getNumRtpSessions();
			sendSetup(nextMediaIndex);
		} else {
			session.init();
			sendPlay();
		}
	}

	private static final StringBuilder toString(HttpHeaders headers) {
		StringBuilder buf = new StringBuilder();
		Iterator<Entry<String, String>> itr = headers.iterator();
		while (itr.hasNext()) {
			Entry<String, String> entry = itr.next();
			buf.append(entry.getKey()).append("=").append(entry.getValue())
					.append(Separators.NEWLINE);
		}

		return buf;
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
}
