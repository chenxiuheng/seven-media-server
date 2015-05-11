package org.zwen.media.file.mts;

import java.util.List;

import javax.media.Format;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.zwen.media.AVPacket;
import org.zwen.media.AVTimeUnit;

public class DefaultPESVisitor implements PESVistor {
	private Format format;
	
	public DefaultPESVisitor(Format format) {
		this.format = format;
	}
	
	@Override
	public void visit(PESPacket pes, List<AVPacket> out) {
		AVPacket packet = new AVPacket();
		
		// the decode time before present picture
		long compositeTime = 0;
		if (pes.dts > 0) {
			compositeTime = pes.pts - pes.dts;
		}
		
		packet.setCompositionTime(compositeTime);
		
		packet.setData(pes.data.array());
		packet.setFormat(format);
		packet.setPts(pes.pts);
		packet.setTimeUnit(AVTimeUnit.MILLISECONDS_90);
		packet.setPosition(pes.pos);
		
		out.add(packet);
	}
	
	@Override
	public void flush(List<AVPacket> out) {
		
	}

}
