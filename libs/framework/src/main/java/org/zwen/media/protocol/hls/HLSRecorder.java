package org.zwen.media.protocol.hls;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVDispatcher;
import org.zwen.media.SystemClock;
import org.zwen.media.Threads;
import org.zwen.media.URLUtils;
import org.zwen.media.file.mts.MTSReader;

public class HLSRecorder extends AVDispatcher implements Closeable {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(HLSRecorder.class);

	private SystemClock clock = new SystemClock();
	private HttpClient client;
	final private String url;
	final private int selectIndex;
	private String baseURL;

	private Iterator<String> tsUrlItr;
	final private File temp;

	public HLSRecorder(String url, File dir) {
		this(url, 0, dir);
	}

	public HLSRecorder(String url, int index, File dir) {
		this.url = url;
		this.selectIndex = index;

		// use file to save temp ts;
		String tempFileName = DateFormatUtils.format(
				System.currentTimeMillis(), "yyyyMMddHHmmss")
				+ ".ts";
		this.temp = new File(dir, tempFileName);
		temp.getParentFile().mkdirs();

		this.client = new HttpClient(new SimpleHttpConnectionManager(false));
		HttpClientParams params = new HttpClientParams();
		params.setSoTimeout(10 * 1000);
		client.setParams(params);

		List<String> tsUrls = Collections.emptyList();
		tsUrlItr = tsUrls.iterator();
	}

	public void connect() throws IOException {
		String url = this.url;
		String m3u8 = readM3U8(url);

		Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF[^\n]+\n([^\n]+)")
				.matcher(m3u8);
		for (int i = 0; i <= selectIndex && matcher.find(); i++) {
			String uri = matcher.group(1);
			url = URLUtils.getAbsoluteUrl(this.url, uri);
		}

		m3u8 = readM3U8(url);
		baseURL = url;

		List<String> tsUrls = new ArrayList<String>();
		matcher = Pattern.compile(
				"(#EXT-X-DISCONTINUITY[^\n]+\n)?#EXTINF[^\n]+\n([^\n]+)")
				.matcher(m3u8);
		while (matcher.find()) {
			boolean isDiscontinuity = null != matcher.group(1);
			String uri = matcher.group(2);
			url = URLUtils.getAbsoluteUrl(this.baseURL, uri);
			LOGGER.info("{} {}", isDiscontinuity ? "discontinue" : "", url);

			tsUrls.add(url);
		}
		
		this.tsUrlItr = tsUrls.iterator();
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

	public boolean hasNextTs() {
		return tsUrlItr.hasNext();
	}

	public boolean readNext() {
		int times = 0;
		final int maxTimes = 5;

		/** try many times to download web resource**/
		String url = tsUrlItr.next();
		for (; times < maxTimes; times++) {
			try {
				temp.delete();
				saveAs(url, temp);
				break;
			} catch (IOException e) {
				LOGGER.warn("Fail download {} {} time", url, times);
			}
		}
		
		if (!temp.exists() || temp.length() < 188 * 2) {
			return false;
		}
		
		/*** download success, read it */
		if (times < maxTimes) {
			MTSReader reader = null;
			try {
				reader = new MTSReader(new FileInputStream(temp).getChannel(),
					clock);
				
				while (-1 != reader.read(this)) {
				}
				
				return true;
			} catch (IOException e) {
				LOGGER.error("Can't Decode read {}", url);
				LOGGER.info(e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(reader);
			}
		}

		/** try too many times **/
		return false;
	}

	private void saveAs(String url, File dst) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		GetMethod get = new GetMethod(url);
		try {
			int status = client.executeMethod(get);
			LOGGER.info("status = {}, {}", status, url);
			ensure200(status, url);

			in = get.getResponseBodyAsStream();

			out = new FileOutputStream(dst);
			IOUtils.copy(in, out);
			
			in.close();
			out.close();
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			get.releaseConnection();
		}
	}

	private boolean readMTS(String url) throws HttpException, IOException {
		GetMethod get = new GetMethod(url);
		int status = client.executeMethod(get);
		LOGGER.info("status = {}, {}", status, url);
		ensure200(status, url);

		PipedOutputStream pipedOut = new PipedOutputStream();
		PipedInputStream pipedIn = new PipedInputStream(pipedOut);

		InputStream in = get.getResponseBodyAsStream();
		final MTSReader reader = new MTSReader(Channels.newChannel(pipedIn),
				clock);

		Future<Boolean> future = Threads.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				while (-1 != reader.read(HLSRecorder.this)) {
				}
				;
				reader.flush(HLSRecorder.this);

				return null;
			}
		});

		// wait
		try {
			Boolean rst = future.get();

			return null != rst ? rst : false;
		} catch (ExecutionException e) {
			LOGGER.warn("Fail Download " + url, e.getCause());
		} catch (Exception e) {
			LOGGER.warn("Fail Download " + url, e);
		} finally {
			IOUtils.copy(in, pipedOut);
		}
		return false;
	}

	/**
	 * make sure the response status code is 200
	 * 
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
		try {
			/** delete temp file */
			temp.delete();
		} finally {
			HttpConnectionManager man = client.getHttpConnectionManager();
			((SimpleHttpConnectionManager) man).shutdown();
		}
	}
}
