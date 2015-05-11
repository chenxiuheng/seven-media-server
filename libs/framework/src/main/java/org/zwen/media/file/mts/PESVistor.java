package org.zwen.media.file.mts;

import java.util.List;

import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.zwen.media.AVPacket;

public interface PESVistor {
	void visit(PESPacket pes, List<AVPacket> out);
	
	void flush(List<AVPacket> out);
}
