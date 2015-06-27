package org.zwen.media.protocol.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.rtp.SessionAddress;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVDispatcher;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.Constants;
import org.zwen.media.SystemClock;
import org.zwen.media.URLUtils;
import org.zwen.media.protocol.PortManager;
import org.zwen.media.rtp.codec.audio.aac.Mpeg4GenericCodec;
import org.zwen.media.rtp.codec.video.h264.H264DePacketizer;

public class RtspClient extends AVDispatcher implements Closeable {
	private static final RtpReceiver[] AVSTREMS_EMPTY = new RtpReceiver[0];

	private static final Logger logger = LoggerFactory
			.getLogger(RtspClient.class);

	private SystemClock sysClock = new SystemClock(700);

	private String url;
	private String user;
	private String pass;
	private RtspConnector stack;
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

		stack = new RtspConnector(host, port);
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
			assertNotNull(mediaDescriptions, "No Media(s)");

			// setup streams
			int streamIndex = 0;
			List<RtpReceiver> streams = new ArrayList<RtpReceiver>();
			Iterator<MediaDescription> iter = (Iterator<MediaDescription>) mediaDescriptions
					.iterator();
			while (iter.hasNext()) {
				MediaDescription md = iter.next();

				String proto = md.getMedia().getProtocol();

				boolean isUdp = "RTP/AVP".equalsIgnoreCase(proto)
						|| "RTP/AVP/UDP".equalsIgnoreCase(proto);
				if (!isUdp) {
					throw new UnsupportedOperationException(
							"unsupported proto [" + proto + "]");
				}

				try {
					RtpReceiver stream = null;
					stream = makeReceiver(streamIndex++, md);
					streams.add(stream);

					boolean connect = setup(stream);
					if (!connect) {
						logger.warn("{} Connect Fail", stream);
					}
				} catch (Exception e) {
					logger.error("error media description {}", md);
					logger.info(e.getMessage(), e);
					e.printStackTrace();
				}

			}
			this.avstreams = streams.toArray(AVSTREMS_EMPTY);
			fireSetup(avstreams);

			// play
			play();

		} catch (SdpException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private RtpReceiver makeReceiver(int streamIndex, MediaDescription md)
			throws SdpParseException {
		Matcher matcher;
		RtpReceiver rtp = new RtpReceiver(streamIndex, sysClock);

		// a=control:rtsp://127.0.0.1:8554/trackID=2
		String control = md.getAttribute("control");
		assertNotNull(control, "no control uri/url");
		rtp.setControlUrl(URLUtils.getAbsoluteUrl(url, control));
		rtp.setMediaType(md.getMedia().getMediaType());

		// a=rtpmap:96 mpeg4-generic/22050/2
		// a=rtpmap:96 H264/90000
		String inputFormat;
		String mediaType = md.getMedia().getMediaType();
		String rtpmap = md.getAttribute("rtpmap");
		assertNotNull(rtpmap, "rtpmap unknown" + md.getMedia());
		matcher = Pattern.compile("(\\d+) ([^/]+)(/(\\d+)(/([^/]+))?)?(.*)?")
				.matcher(rtpmap);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("rtpmap[" + rtpmap
					+ "] is not legal format");
		} else {
			Integer payloadType = Integer.valueOf(matcher.group(1));
			Integer clockRate = Integer.valueOf(matcher.group(4));
			rtp.setTimeUnit(AVTimeUnit.valueOf(clockRate));
			rtp.setPayloadType(payloadType);
			inputFormat = matcher.group(2);
			if ("h264".equalsIgnoreCase(inputFormat)) {
				rtp.setDePacketizer(new H264DePacketizer());
				rtp.setFormat(new VideoFormat(Constants.H264));
			} else if ("mpeg4-generic".equalsIgnoreCase(inputFormat)) {
				rtp.setDePacketizer(new Mpeg4GenericCodec());
				rtp.setFormat(new AudioFormat(Constants.AAC));
			} else {
				rtp.setDePacketizer(null);
				if ("video".equals(mediaType)) {
					rtp.setFormat(new VideoFormat(inputFormat));
				} else {
					rtp.setFormat(new AudioFormat(inputFormat));
				}
			}
		}

		// a=fmtp:96
		// packetization-mode=1;profile-level-id=64001e;sprop-parameter-sets=Z2QAHqzIYCoMfkwEQAAAAwBAAAAMo8WLZ4A=,aOm7LIs=;
		// a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr;
		// config=139056e5a54800; SizeLength=13; IndexLength=3;
		// IndexDeltaLength=3; Profile=1;
		String fmtpValue = md.getAttribute("fmtp");
		if (null == fmtpValue) {
			logger.warn("Not Found fmtp for {}", md.getMedia());
			return rtp;
		}
		matcher = Pattern.compile("([\\d]+)([\\s]+)(.+)").matcher(fmtpValue);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(fmtpValue + " is unknown");
		} else if (null != rtp.getDePacketizer() && null != fmtpValue) {
			rtp.getDePacketizer().init(rtp, fmtpValue);
		}

		return rtp;
	}

	private void assertNotNull(Object obj, String message) {
		if (null == obj) {
			throw new ChannelException(message);
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

	private boolean setup(RtpReceiver stream) throws IOException {
		// make request
		final String controlUrl = stream.getControlUrl();
		DefaultHttpRequest request = new DefaultHttpRequest(
				RtspVersions.RTSP_1_0, RtspMethods.SETUP, controlUrl);

		int[] ports = PortManager.findAvailablePorts(2);
		String transport = String.format("RTP/AVP;unicast;client_port=%d-%d",
				ports[0], ports[1]);
		request.headers().add(RtspHeaders.Names.TRANSPORT, transport);

		// handle response
		HttpResponse resp = stack.send(request).get();
		if (null == resp) {
			logger.warn("fail setup {}", controlUrl);
			return false;
		}
		HttpHeaders headers = resp.headers();

		// Transport=RTP/AVP/UDP;unicast;client_port=6794-6795;server_port=63837-63838;ssrc=6C467D5B;mode=play
		// Transport=RTP/AVP;unicast;client_port=6794-6795;server_port=63837-63838;ssrc=6C467D5B;mode=play
		transport = headers.get(RtspHeaders.Names.TRANSPORT);
		if (!StringUtils.startsWithIgnoreCase(transport, "RTP/AVP/UDP;unicast")
				&& !StringUtils.startsWithIgnoreCase(transport,
						"RTP/AVP;unicast")) {
			logger.error("can't support {}", transport);
			return false;
		}

		// UDP Transport
		String client_port_0 = String.valueOf(ports[0]);
		String client_port_1 = String.valueOf(ports[1]);
		String server_port_0 = null;
		String server_port_1 = null;
		int ssrc = 0;

		Matcher matcher = Pattern.compile("([^\\s=;]+)=(([^-;]+)(-([^;]+))?)")
				.matcher(transport);
		while (matcher.find()) {
			String key = matcher.group(1).toLowerCase();
			if ("client_port".equals(key)) {
				client_port_0 = matcher.group(3);
				client_port_1 = matcher.group(5);
			} else if ("server_port".equals(key)) {
				server_port_0 = matcher.group(3);
				server_port_1 = matcher.group(5);
			} else if ("ssrc".equals(key)) {
				ssrc = (int) Long.parseLong(matcher.group(2), 16);
			} else {
				logger.warn("ignored [{}={}]", key, matcher.group(2));
			}
		}

		SessionAddress localParticipant = null;
		SessionAddress remoteParticipant = null;

		// make rtp session and init
		String sndHost = stack.getHost();
		Integer sndRTPPort = Integer.valueOf(server_port_0);
		Integer sndRTCPPort = Integer.valueOf(server_port_1);
		remoteParticipant = new SessionAddress(InetAddress.getByName(sndHost),
				sndRTPPort);

		// receiver
		Integer recvRTPPort = Integer.valueOf(client_port_0);
		Integer recvRTCPPort = Integer.valueOf(client_port_1);
		localParticipant = new SessionAddress(InetAddress.getLocalHost(),
				recvRTPPort, recvRTCPPort);

		// rtp receive connect it
		boolean connected = false;
		try {
			connected = stream.connect(stack.getSessionId(), ssrc,
					localParticipant, remoteParticipant, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!connected) {
			throw new IOException("fail connect " + transport);
		}

		// send heart beat
		String sessionId = headers.get(RtspHeaders.Names.SESSION);
		if (null != sessionId) {
			matcher = Pattern.compile("([^;]+)(.*(timeout=([\\d]+)).*)?")
					.matcher(sessionId);
			if (matcher.matches()) {
				String timeout = matcher.group(4);
				int delay = 1000 * 3;
				int period = 1000 * Integer.valueOf(timeout);
				timer.schedule(new java.util.TimerTask() {

					@Override
					public void run() {
						HttpResponse response;
						if (supportGetParameters) {
							response = getParameters(controlUrl);
						} else {
							response = option(null, null);
						}

						if (response.getStatus().getCode() > 400) {
							logger.error("{} from {}", response.getStatus(),
									controlUrl);
							timer.cancel();
						}
					}
				}, delay, period);
			}
		}

		return true;
	}

	private void play() {
		Matcher matcher;
		HttpRequest request = makeRequest(RtspMethods.PLAY);
		request.headers().set(RtspHeaders.Names.RANGE, "npt=0.000-");

		HttpResponse response = stack.send(request).get();

		String rtpInfo = response.headers().get("RTP-Info");
		String[] infos = StringUtils.split(rtpInfo, ",");
		for (int i = 0; i < infos.length && i < avstreams.length; i++) {

			String url = null;
			int seq = 0;
			long rtptime = 0;

			String info = infos[i];
			matcher = Pattern.compile("([a-zA-Z][^=]+)=([^;]+)(;)?").matcher(
					info);
			while (matcher.find()) {
				String key = matcher.group(1);
				String value = matcher.group(2);
				if (key.equalsIgnoreCase("url")) {
					url = value;
				} else if (key.equalsIgnoreCase("seq")) {
					seq = Integer.valueOf(value);
				} else if (key.equalsIgnoreCase("rtptime")) {
					rtptime = Long.valueOf(value);
				}
			}

			for (int j = 0; j < avstreams.length; j++) {
				RtpReceiver receiver = avstreams[j];
				if (StringUtils.equalsIgnoreCase(receiver.getControlUrl(), url)) {
					receiver.setRtpTime(rtptime);
					receiver.setSeq(seq);
				}
			}
		}
	}

	private void tearDown() {
		HttpRequest request = makeRequest(RtspMethods.TEARDOWN);

		stack.send(request).get();
	}

	private HttpResponse getParameters(String controlUrl) {
		DefaultHttpRequest request = new DefaultHttpRequest(
				RtspVersions.RTSP_1_0, RtspMethods.GET_PARAMETER, controlUrl);

		HttpResponse response = stack.send(request).get();
		return response;
	}

	private DefaultHttpRequest makeRequest(HttpMethod method) {
		return new DefaultHttpRequest(RtspVersions.RTSP_1_0, method, url);
	}

	public static String getAuthValue(String user, String pass) {
		byte[] auth = Base64.encodeBase64(new String(user + ":"
				+ (pass != null ? pass : "")).getBytes());
		String authValue = "Basic " + new String(auth);
		return authValue;
	}

	@Override
	public void close() {
		try {
			// inner jobs
			timer.cancel();

			// data receive jobs
			for (RtpReceiver rtp : avstreams) {
				if (null == rtp.getSession()) {
					continue;
				}

				try {
					rtp.getSession().terminate();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}

			// rtsp connection
			if (null != stack) {
				tearDown();
			}
		} finally {
			if (null != stack) {
				stack.close();
			}
		}

		fireClosed();
	}

}
