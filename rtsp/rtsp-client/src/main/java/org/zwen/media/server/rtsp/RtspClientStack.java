package org.zwen.media.server.rtsp;

import gov.nist.core.Separators;

import java.io.Closeable;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.rtsp.RtspHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtspClientStack implements Closeable {
	final private static ExecutorService threadPool = Executors
			.newCachedThreadPool();
	final private static Logger logger = LoggerFactory
			.getLogger(RtspClientStack.class);

	final private String host;
	final private int port;

	private String sessionId;
	private String authValue;
	private AtomicLong cseq = new AtomicLong(1);
	private Channel channel;
	private ClientBootstrap bootstrap;
	private ConcurrentHashMap<String, AsynFuture> futures = new ConcurrentHashMap<String, AsynFuture>();

	public RtspClientStack(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	public AsynFuture send(HttpRequest request) {
		long seqNo = cseq.getAndIncrement();
		AsynFuture f = new AsynFuture();

		HttpHeaders headers = request.headers();
		headers.add(RtspHeaders.Names.CSEQ, seqNo);
		if (null != sessionId) {
			headers.add(RtspHeaders.Names.SESSION, sessionId);
		}

		if (headers.contains(RtspHeaders.Names.AUTHORIZATION)) {
			authValue = headers.get(RtspHeaders.Names.AUTHORIZATION);
		} else if (null != authValue) {
			headers.add(RtspHeaders.Names.AUTHORIZATION, authValue);
		}

		futures.put(String.valueOf(seqNo), f);
		channel.write(request);

		if (logger.isInfoEnabled()) {
			logger.info(request.getMethod() + " {}\r\n{}\r\n",
					request.getUri(), toString(headers));
		}

		return f;
	}

	void onResponse(HttpResponse response) {
		if (logger.isInfoEnabled()) {
			HttpHeaders headers = response.headers();
			String content = "";
			if (response.getContent().readableBytes() > 0) {
				byte[] bytes = new byte[response.getContent().readableBytes()];
				response.getContent().readBytes(bytes);
				content = new String(bytes);
			}

			logger.info("{}\r\n{}", toString(headers), content);
		}

		String sessionId = response.headers().get(RtspHeaders.Names.SESSION);
		if (null != sessionId) {
			this.sessionId = sessionId;
		}

		String seqNo = response.headers().get(RtspHeaders.Names.CSEQ);
		AsynFuture f = futures.remove(seqNo);
		if (null != f) {
			f.handle(response);
		} else {
			throw new ChannelException("Unknown CSEQ[" + seqNo + "]");
		}
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

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getSessionId() {
		return sessionId;
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

	public static final class AsynFuture implements Serializable {
		private static final long serialVersionUID = 1L;

		private HttpResponse response;
		private Semaphore lock = new Semaphore(0);

		void handle(HttpResponse response) {
			lock.release();

			this.response = response;
		}

		public HttpResponse get() throws ChannelException {
			try {
				lock.acquire();
				lock.release();
			} catch (InterruptedException e) {
				handleInterruptedException(e);
			}

			return response;
		}

		public HttpResponse get(long timeout, TimeUnit unit)
				throws ChannelException {
			try {
				boolean got = lock.tryAcquire(timeout, unit);
				if (got) {
					lock.release();
				}
			} catch (InterruptedException e) {
				handleInterruptedException(e);
			}

			return response;
		}

		private void handleInterruptedException(InterruptedException ex)
				throws ChannelException {
			throw new ChannelException(ex.getMessage(), ex.getCause());
		}
	}
}