package org.zwen.media.server.rtsp;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class RtspRequestFuture {
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
