package org.zwen.media.rtp.codec;

import java.util.List;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import org.zwen.media.AVPacket;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.AVTimeUnit;

import com.biasedbit.efflux.packet.DataPacket;

public interface IDePacketizer {
	/**
	 * a=fmtp:<format> <format specific parameters><br/>
	 * 
	 *  This attribute allows parameters that are specific to a
     *  particular format to be conveyed in a way that SDP does not
     *  have to understand them.  The format must be one of the formats
     *  specified for the media.  Format-specific parameters may be any
     *  set of parameters required to be conveyed by SDP and given
     *  unchanged to the media tool that will use this format.  At most
     *  one instance of this attribute is allowed for each format.
     *  It is a media-level attribute, and it is not dependent on
     *  charset.
	 */
	public static final String FMTP = "fmtp";
	
	/**
	 * a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding 
	 *   parameters>] <br/>
	 * 
	 * This attribute maps from an RTP payload type number (as used in
	 * an "m=" line) to an encoding name denoting the payload format
	 * to be used.  It also provides information on the clock rate and
	 * encoding parameters.  It is a media-level attribute that is not
	 * dependent on charset.
	 */
	public static final String RTPMAP = "rtpmap";
	
	public static final int RTP_MAX_PACKET_LENGTH = 1460;
	
	public void process(DataPacket packet, List<AVPacket> out);
	
	public AVStreamExtra depacketize(MediaDescription md) throws SdpException;
	
	public void setTimeUnit(AVTimeUnit timeUnit) ;
}
