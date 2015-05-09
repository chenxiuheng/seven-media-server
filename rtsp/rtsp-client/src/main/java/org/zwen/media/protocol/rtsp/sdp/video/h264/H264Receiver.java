package org.zwen.media.protocol.rtsp.sdp.video.h264;



import java.util.concurrent.atomic.AtomicLong;

import javax.media.format.VideoFormat;
import javax.sdp.MediaDescription;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.Constants;
import org.zwen.media.protocol.rtsp.RtpReceiver;
import org.zwen.media.rtp.codec.video.h264.DePacketizer;


public class H264Receiver extends RtpReceiver {
	private static Logger logger = LoggerFactory.getLogger(H264Receiver.class);
	
	public H264Receiver(AtomicLong sysClock, MediaDescription md) {
		super(sysClock, new VideoFormat(Constants.H264_RTP), md, new DePacketizer());
	}


	@Override
	public AVStreamExtra getExtra() {
		H264AVStreamExtra extra = new H264AVStreamExtra();
		
		FmtpValues fmtp = getFmtpAttribute();
		if (null == fmtp) {
			return null;
		}
		String profile = fmtp.getValue("profile-level-id");
		try {
			extra.setProfile(Hex.decodeHex(profile.toCharArray()));
		} catch (DecoderException e) {
			logger.error("fail to decode {}", profile);
		}
		
		String sps_pps = fmtp.getValue("sprop-parameter-sets");
		if (null != sps_pps && sps_pps.contains(",")) {
			String[] segs = StringUtils.split(sps_pps, ',');
			byte[] sps = Base64.decodeBase64(segs[0].getBytes());
			byte[] pps = Base64.decodeBase64(segs[1].getBytes());
			
			extra.setSps(new byte[][]{sps});
			extra.setPps(new byte[][]{pps});
	
			logger.warn("sps.byte[{}] = {}", sps.length, Hex.encodeHex(sps));
			logger.warn("pps.byte[{}] = {}", pps.length, Hex.encodeHex(pps));
		}
		return extra;
	}

}
