package org.zwen.media.protocol.hls;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jcodec.containers.mps.MTSUtils.StreamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStreamDispatcher;
import org.zwen.media.Constants;
import org.zwen.media.Threads;
import org.zwen.media.URLUtils;
import org.zwen.media.file.mts.DefaultPESVisitor;
import org.zwen.media.file.mts.MTSReader;
import org.zwen.media.file.mts.PESVistor;
import org.zwen.media.file.mts.vistor.H264Visitor;


public class HLSClient extends AVStreamDispatcher implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(HLSClient.class);
	
	private boolean isClosed;
	
	private HttpClient client;
	final private String url;
	final private int selectIndex;
	private String selectedM3U8;
	private String baseURL;

	public HLSClient(HttpClient client, String url) {
		this(url, 0);
		
		this.client = client;
		HttpClientParams params = new HttpClientParams();
		params.setSoTimeout(10 * 1000);
		client.setParams(params);
	}
	
	public HLSClient(String url, int streamIndex) {
		this.url = url;
		this.selectIndex = streamIndex;
	}

	public void connect() throws IOException {
		String url = this.url;
		String m3u8 = readM3U8(url);
		

		Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF[^\n]+\n([^\n]+)").matcher(m3u8);
		for (int i = 0; i <= selectIndex && matcher.find(); i++) {
			String uri = matcher.group(1);
			url = URLUtils.getAbsoluteUrl(this.url, uri);
		}
		
		selectedM3U8 = readM3U8(url);
		baseURL = url;
	}

	private String readM3U8(String url) throws IOException, HttpException {
		HttpMethod get = new GetMethod(url);
		int status = client.executeMethod(get);
		LOGGER.info("status = {}, {}", status, url);
		ensure200(status, url);

		String m3u8 = get.getResponseBodyAsString();
		if (!isM3U8(m3u8)) {
			throw new IllegalArgumentException("Not M3U8" + m3u8);
		}
		
		LOGGER.info("\n{}", m3u8);
		return m3u8;
	}

	public void start() {
		String url;
		Matcher matcher = Pattern.compile("(#EXT-X-DISCONTINUITY[^\n]+\n)?#EXTINF[^\n]+\n([^\n]+)").matcher(selectedM3U8);
		while (matcher.find()) {
			boolean isDiscontinuity = null != matcher.group(1);
			String uri = matcher.group(2);
			url = URLUtils.getAbsoluteUrl(this.baseURL, uri);
			LOGGER.info("{} {}", isDiscontinuity ? "discontinue":"", url);
			
			try {
				readMTS(url);
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void readMTS(String url) throws HttpException, IOException {
		GetMethod get = new GetMethod(url);
		int status = client.executeMethod(get);
		LOGGER.info("status = {}, {}", status, url);
		ensure200(status, url);
		
		PipedOutputStream pipedOut = new PipedOutputStream();
		PipedInputStream pipedIn = new PipedInputStream(pipedOut);
		
		Map<StreamType, PESVistor> visitors = new HashMap<StreamType, PESVistor>();
		visitors.put(StreamType.AUDIO_AAC_ADTS, new DefaultPESVisitor(new AudioFormat(Constants.AAC_ADTS)));
		visitors.put(StreamType.VIDEO_H264, new H264Visitor());
		InputStream in = get.getResponseBodyAsStream();
		final MTSReader reader = new MTSReader(Channels.newChannel(pipedIn), visitors);
		
		Threads.submit(new Runnable() {
			private List<AVPacket> pkts = new ArrayList<AVPacket>();
			@Override
			public void run() {
				try {
					while (-1 != reader.readNextMPEGPacket(pkts)) {
						if (pkts.isEmpty()) {
							continue;
						}
						
						AVPacket packet = pkts.remove(0);
						System.out.println(packet);
					}
					;
					reader.flush(pkts);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		IOUtils.copy(in, pipedOut);
	}
	
	/**
	 * make sure the response status code is 200
	 * @param status
	 * @param url
	 * @throws IOException
	 */
	private void ensure200(int status, String url) throws IOException {
		if (status != 200) {
			throw new IOException("Error Code: " + status + "," + url);
		}
	}
	
	private boolean isM3U8(String content) {
		return StringUtils.startsWith(content, "#EXTM3U");
	}
	
	@Override
	public void close() throws IOException {
		isClosed = true;
	}
}