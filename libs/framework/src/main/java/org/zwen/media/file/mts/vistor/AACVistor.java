package org.zwen.media.file.mts.vistor;

import java.util.Collection;

import org.jcodec.containers.mps.MPSDemuxer.PESPacket;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.file.mts.PESVistor;

public class AACVistor implements PESVistor {



	@Override
	public void visit(AVStream av, PESPacket pes, Collection<AVPacket> out) {
		
	
	}
	
	@Override
	public void flush(AVStream av, Collection<AVPacket> out) {
		
	}
}
