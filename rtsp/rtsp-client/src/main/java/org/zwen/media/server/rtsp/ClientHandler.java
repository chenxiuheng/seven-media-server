package org.zwen.media.server.rtsp;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.zwensoft.mediaserver.rtsp.RtspClient;

/**
 * @author res
 */
public class ClientHandler extends SimpleChannelUpstreamHandler {
	private RtspClientStack client;

	public ClientHandler(RtspClientStack client) {
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