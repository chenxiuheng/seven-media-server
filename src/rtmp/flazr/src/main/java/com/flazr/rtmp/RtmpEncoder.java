/*
 * Flazr <http://flazr.com> Copyright (C) 2009  Peter Thomas.
 *
 * This file is part of Flazr.
 *
 * Flazr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Flazr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Flazr.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.flazr.rtmp;

import com.flazr.rtmp.message.ChunkSize;
import com.flazr.rtmp.message.Control;
import com.flazr.rtmp.message.MessageType;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelPipelineCoverage("one")
public class RtmpEncoder extends SimpleChannelDownstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(RtmpEncoder.class);

    private int chunkSize = 128;    
    private RtmpHeader[] channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];    

    private void clearPrevHeaders() {
        logger.debug("clearing prev stream headers");
        channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];
    }

    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) {
    	if (e.getMessage() instanceof RtmpMessage) {
    		Channels.write(ctx, e.getFuture(), encode((RtmpMessage) e.getMessage()));
    	} else {
    		RtmpMessage[] msgs = (RtmpMessage[])e.getMessage();
    		ChannelBuffer buf = ChannelBuffers.buffer(1024);
    		for (int i = 0; i < msgs.length; i++) {
    			RtmpMessage msg = msgs[i];
    			buf.writeBytes(encode(msg));
			}

    		Channels.write(ctx, e.getFuture(), buf);
    	}
    }

    public ChannelBuffer encode(final RtmpMessage message) {
        final ChannelBuffer in = message.encode();
        final RtmpHeader header = message.getHeader();
        if(header.isChunkSize()) {
            final ChunkSize csMessage = (ChunkSize) message;
            logger.debug("encoder new chunk size: {}", csMessage);
            chunkSize = csMessage.getChunkSize();
        } else if(header.isControl()) {
            final Control control = (Control) message;
            if(control.getType() == Control.Type.STREAM_BEGIN) {
                clearPrevHeaders();
            }
        }
        final int channelId = header.getChannelId();
        header.setSize(in.readableBytes());
        final RtmpHeader prevHeader = channelPrevHeaders[channelId];       
        if(prevHeader != null // first stream message is always large
                && header.getStreamId() > 0 // all control messages always large
                && header.getTime() > 0) { // if time is zero, always large
            if(header.getSize() == prevHeader.getSize()) {
                header.setHeaderType(RtmpHeader.Type.SMALL);
            } else {
                header.setHeaderType(RtmpHeader.Type.MEDIUM);
            }
            final int deltaTime = header.getTime() - prevHeader.getTime();
            if(deltaTime < 0) {
                logger.warn("negative time: {}", header);
                header.setDeltaTime(0);
            } else {
                header.setDeltaTime(deltaTime);
            }
        } else {
			// otherwise force to LARGE
            header.setHeaderType(RtmpHeader.Type.LARGE);
        }
        channelPrevHeaders[channelId] = header;        
        if(logger.isDebugEnabled()) {
        	// don't print millions of PING_RESPONSE
        	if (message.getHeader().getMessageType() != MessageType.CONTROL || ((Control) message).getType() != Control.Type.PING_RESPONSE)
        		logger.debug(">> {}", message);
        }                
        final ChannelBuffer out = ChannelBuffers.buffer(
                RtmpHeader.MAX_ENCODED_SIZE + header.getSize() + header.getSize() / chunkSize);
        boolean first = true;
        while(in.readable()) {
            final int size = Math.min(chunkSize, in.readableBytes());
            if(first) {                
                header.encode(out);
                first = false;
            } else {                
                out.writeBytes(header.getTinyHeader());
            }
            in.readBytes(out, size);
        }
        return out;
    }

}
