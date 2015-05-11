package org.zwen.media.protocol.hls;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;

import junit.framework.TestCase;

public class TestHLSClient extends TestCase {
	public void testConnect() throws IOException {
		String url = "http://newmedia.chinacourt.org/vod/play/2015/04/20/15/fe28e38e396e2ab9877c68ed51bf3d91/playlist.m3u8";

		HLSClient client = new HLSClient(new HttpClient(), url);
		client.connect();
		
		client.start();
	}
}
