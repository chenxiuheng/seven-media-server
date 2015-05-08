package org.zwen.media.rtp.codec;

import java.util.List;

import org.zwen.media.AVPacket;

import com.biasedbit.efflux.packet.DataPacket;

public interface IDePacketizer {
	public void process(DataPacket packet, List<AVPacket> out);
}
