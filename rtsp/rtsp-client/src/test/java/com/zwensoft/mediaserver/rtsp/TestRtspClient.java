package com.zwensoft.mediaserver.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.media.Buffer;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.core.MediaFrame;
import org.zwen.media.core.file.flv.FlvWriter;
import org.zwen.media.server.rtp.video.h264.DePacketizer;
import org.zwen.media.server.rtp.video.h264.FmtpValues;
import org.zwen.media.server.rtp.video.h264.H264AVStream;
import org.zwen.media.server.rtsp.NoPortAvailableException;
import org.zwen.media.server.rtsp.RtspClient;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionDataListener;

public class TestRtspClient extends TestCase {
	private static final Logger logger = LoggerFactory
			.getLogger(TestRtspClient.class);

	public void testDecodeSdp() throws ParseException, SdpException {
		String text = "v=0\n"
				+ "o=- 2251938191 2251938191 IN IP4 0.0.0.0\n"
				+ "s=RTSP Session of ZheJiang Dahua Technology CO.,LTD.\n"
				+ "c=IN IP4 0.0.0.0\n"
				+ "t=0 0\n"
				+ "a=control:*\n"
				+ "a=range:npt=now-\n"
				+ "a=packetization-supported:DH\n"
				+ "m=video 0 RTP/AVP 96\n"
				+ "a=control:trackID=0\n"
				+ "a=framerate:25.000000\n"
				+ "a=rtpmap:96 H264/90000\n"
				+ "a=fmtp:96 packetization-mode=1;profile-level-id=4D001F;sprop-parameter-sets=Z00AH9oBQBbpUgAAAwACAAADAGTAgAC7fgAD9H973wvCIRqA,aM48gA==\n"
				+ "m=audio 0 RTP/AVP 8\n" + "a=control:trackID=1\n"
				+ "a=rtpmap:8 PCMA/8000\n";

		SessionDescriptionImpl sd = new SessionDescriptionImpl();

		StringTokenizer tokenizer = new StringTokenizer(text);
		while (tokenizer.hasMoreChars()) {
			String line = tokenizer.nextToken();

			SDPParser paser = ParserFactory.createParser(line);
			SDPField obj = paser.parse();
			sd.addField(obj);
		}

		Vector<MediaDescription> ms = sd.getMediaDescriptions(false);
		MediaDescription one = ms.get(0);
		System.out.println(one);
	}

	public void testConnect() throws NoPortAvailableException, SdpException,
			InterruptedException, IOException {
		String url = "rtsp://172.16.160.200:554";
		RtspClient client = new RtspClient(url, "admin", "admin");
		client.connect();
		List<H264AVStream> streams = client.getStreams();
		for (H264AVStream avStream : streams) {
			FmtpValues values = avStream.getFmtpAttribute();
			if (null == values) {
				continue;
			}

			String profile = values.getValue("profile-level-id");
			logger.warn("profile = {}", profile);

			String sps_pps = values.getValue("sprop-parameter-sets");
			if (null != sps_pps && sps_pps.contains(",")) {
				String[] segs = StringUtils.split(sps_pps, ',');
				byte[] sps = Base64.decodeBase64(segs[0].getBytes());
				byte[] pps = Base64.decodeBase64(segs[1].getBytes());

				logger
						.warn("sps.byte[{}] = {}", sps.length, Hex
								.encodeHex(sps));
				logger
						.warn("pps.byte[{}] = {}", pps.length, Hex
								.encodeHex(pps));
			}
		}

		final FlvWriter writer;
		File file = new File("test.flv");
		writer = new FlvWriter(new FileOutputStream(file).getChannel());
		
		writer.setStreams(streams);
		
		writer.writeHead();
		

		client.start(new RtpSessionDataListener() {
			private DePacketizer dePacketizer = new DePacketizer();
			private List<MediaFrame> out = new LinkedList<MediaFrame>();
			boolean isFirst = true;
			long ptsOffset = 0;

			@Override
			public void dataPacketReceived(RtpSession session,
					RtpParticipantInfo participant, DataPacket packet) {
				dePacketizer.depacket(packet, out);
				if (!out.isEmpty()) {
					MediaFrame buf = out.remove(0);
					logger.warn("key={}, t={}.{}, l= {}", new Object[] {
							(buf.getFlags() & Buffer.FLAG_KEY_FRAME) > 0,
							buf.getTimeStamp() / (90 * 1000),
							buf.getTimeStamp() / 90 % 1000, buf.getLength() });
					
					if (isFirst && !buf.isKeyFrame()) {
						System.out.println("I am waiting first key frame");
						return;
					} else if (isFirst) {
						isFirst = false;
						ptsOffset = buf.getTimeStamp();
					}

					try {
						buf.setTimeStamp(buf.getTimeStamp() - ptsOffset);
						writer.write(buf);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Thread.sleep(3 * 60 * 1000);
		client.close();
	}
}
