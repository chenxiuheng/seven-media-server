/**
 * 
 */
package org.zwen.media.file.mts;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.zwen.media.AVPacket;

public class PES {
	private AtomicLong counter;
	private PESVistor visitor;

	private long pesIndex;
	private PESPacket pes;
	private ChannelBuffer buf = ChannelBuffers.EMPTY_BUFFER;

	public PES(AtomicLong counter, PESVistor visitor) {
		this.counter = counter;
		this.visitor = visitor;
	}

	public void readPESHeader(long position, ByteBuffer seg, List<AVPacket> out) {
		if (null != pes) {
			visit(visitor, out);
		}

		pesIndex = counter.getAndIncrement();
		pes = MPSUtils.readPESHeader(seg, 0);
		pes.pos = position;
		append(seg);
	}

	public void flush(List<AVPacket> out) {
		if (null != pes) {
			visit(visitor, out);
		}

		if (null != visitor) {
			visitor.flush(out);
		}

		pes = null;
	}

	public void append(ByteBuffer seg) {
		if (buf.writableBytes() < seg.limit()) {
			ChannelBuffer newBuf = ChannelBuffers.buffer(184 + 2 * (buf
					.readableBytes() + seg.limit()));
			newBuf.writeBytes(buf);
			buf = newBuf;
		}

		buf.writeBytes(seg);
	}

	public void visit(PESVistor visitor, List<AVPacket> out) {
		if (null != visitor && buf.readableBytes() > 0) {
			ByteBuffer data = ByteBuffer.allocate(buf.readableBytes());
			buf.readBytes(data);
			data.flip();
			
			pes.data = data;
			visitor.visit(pes, out);
		}
		
		pes = null;
		buf.clear();
	}
}