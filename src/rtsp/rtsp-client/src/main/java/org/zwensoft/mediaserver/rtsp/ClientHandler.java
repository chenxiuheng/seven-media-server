package org.zwensoft.mediaserver.rtsp;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * @author res
 */
public class ClientHandler extends SimpleChannelUpstreamHandler {
	private RtspClient client;

	public ClientHandler(RtspClient client) {
		this.client = client;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (e.getMessage() instanceof HttpResponse) {
			client.onResponse((HttpResponse) e.getMessage());
		} else {
			super.messageReceived(ctx, e);
		}
	}
}