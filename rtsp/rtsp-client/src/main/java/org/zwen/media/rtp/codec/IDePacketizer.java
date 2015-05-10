package org.zwen.media.rtp.codec;

import java.util.List;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import org.zwen.media.AVPacket;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.AVTimeUnit;

import com.biasedbit.efflux.packet.DataPacket;

public interface IDePacketizer {
	public void process(DataPacket packet, List<AVPacket> out);
	
	public AVStreamExtra depacketize(MediaDescription md) throws SdpException;
	
	public void setTimeUnit(AVTimeUnit timeUnit) ;
}
