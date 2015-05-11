package org.zwen.media.protocol.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVStreamListener;
import org.zwen.media.file.flv.FlvWriter;
import org.zwen.media.protocol.rtsp.NoPortAvailableException;
import org.zwen.media.protocol.rtsp.RtspClient;

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

	public void testDh() throws NoPortAvailableException, SdpException,
			InterruptedException, IOException {
		String url = "rtsp://172.16.160.200:554";
		RtspClient client = new RtspClient(url, "admin", "admin");

		client.addListener(new AVStreamListener() {
			final FlvWriter writer;
			{
				File file = new File("test.flv");
				writer = new FlvWriter(new FileOutputStream(file).getChannel());
			}

			@Override
			public void onSetup(AVStream[] streams) {
				writer.setStreams(Arrays.asList(streams));
				try {
					writer.writeHead();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onPacket(AVStream stream, AVPacket packet) {
				try {
					writer.write(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onClosed() {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		client.connect();
		client.start();

		Thread.sleep(3 * 60 * 1000);
		client.close();
	}
	
	public void testVlc() throws InterruptedException, IOException {

		String url = "rtsp://localhost:8554/";
		RtspClient client = new RtspClient(url, null, null);

		client.addListener(new AVStreamListener() {
			final FlvWriter writer;
			{
				File file = new File("vlc.flv");
				writer = new FlvWriter(new FileOutputStream(file).getChannel());
			}

			@Override
			public void onSetup(AVStream[] streams) {
				writer.setStreams(Arrays.asList(streams));
				try {
					writer.writeHead();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onPacket(AVStream stream, AVPacket packet) {
				try {
					writer.write(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onClosed() {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		client.connect();
		client.start();

		Thread.sleep(3 * 60 * 1000);
		client.close();
	}
}
