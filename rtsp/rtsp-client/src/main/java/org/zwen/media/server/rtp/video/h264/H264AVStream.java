package org.zwen.media.server.rtp.video.h264;

import gov.nist.javax.sdp.fields.MediaField;

import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.core.AVStream;
import org.zwen.media.core.AVStreamExtra;

public class H264AVStream extends AVStream {
	private static final Logger logger = LoggerFactory
			.getLogger(H264AVStream.class);
	
	private MediaDescription md;

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
	
	public H264AVStream(MediaDescription md) {
		this.md = md;
	}

	public MediaField getMedia() {
		try {
			String a = md.getAttribute("m");
			if (null == a) {
				return null;
			}

			MediaField field  = new MediaField();

			String[] valus = StringUtils.split(a, ' ');
			if (valus.length > 0) {
				field.setMedia(valus[0]);
			}
			if (valus.length > 1) {
				field.setNports(Integer.valueOf(valus[1]));
			}
			
			if (valus.length > 2) {
				field.setProto(valus[3]);
			}
			
			Vector<String> formats = new Vector<String>();
			for (int i = 3; i < valus.length; i++) {
				formats.add(valus[i]);
			}
			field.setFormats(formats);
			return field;
		} catch (SdpParseException e) {
			logger.warn("msg = {}, type = {}", e.getMessage(), e.getClass());
		}
		
		return null;
	}
	
	public FmtpValues getFmtpAttribute() {
		FmtpValues attr = null;
		
		try {
			String fmtp = md.getAttribute(FmtpValues.FMTP);
			if (null != fmtp) {
				attr = FmtpValues.parse(fmtp);
			}
		} catch (SdpParseException e) {
			logger.warn("msg = {}, type = {}", e.getMessage(), e.getClass());
		}

		return attr;
	}
	
	@Override
	public String toString() {
		return String.valueOf(md);
	}


}
