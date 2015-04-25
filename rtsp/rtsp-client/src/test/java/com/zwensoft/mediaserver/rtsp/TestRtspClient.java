package com.zwensoft.mediaserver.rtsp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;

import java.text.ParseException;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import junit.framework.TestCase;

import org.jboss.netty.channel.ChannelFuture;
import org.zwensoft.mediaserver.rtsp.RtspClient;

public class TestRtspClient extends TestCase {

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

	public void testConnect() {
		String url = "rtsp://172.16.160.200:554";
		RtspClient client = new RtspClient(url, "admin", "admin");
		ChannelFuture future = client.connect();

		client.start();

		future.awaitUninterruptibly();
		System.out.println(client);
	}
}
