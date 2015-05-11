package org.zwen.media.file.mts.vistor;

import java.util.List;

import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.zwen.media.AVPacket;
import org.zwen.media.file.mts.PESVistor;

public class AACVistor implements PESVistor {



	@Override
	public void visit(PESPacket pes, List<AVPacket> out) {
		
	
	}
	
	@Override
	public void flush(List<AVPacket> out) {
		
	}
}
