package org.zwen.media.protocol.rtsp;

import gov.nist.javax.sdp.fields.MediaField;

import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVStream;
import org.zwen.media.protocol.rtsp.sdp.video.h264.FmtpValues;
import org.zwen.media.rtp.codec.IDePacketizer;

import com.biasedbit.efflux.session.RtpSession;

public abstract class RTPAVStream extends AVStream {

	private static final Logger logger = LoggerFactory
			.getLogger(RTPAVStream.class);
	protected MediaDescription md;
	private RtpSession session;
	private IDePacketizer dePacketizer;

	public RTPAVStream(MediaDescription md) {
		this.md = md;
	}

	public boolean isVideo() {
		return false;
	}
	
	public boolean isAudio() {
		return true;
	}
	
	public MediaField getMedia() {
		try {
			String a = md.getAttribute("m");
			if (null == a) {
				return null;
			}

			MediaField field = new MediaField();

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

	public void setDePacketizer(IDePacketizer dePacketizer) {
		this.dePacketizer = dePacketizer;
	}

	public IDePacketizer getDePacketizer() {
		return dePacketizer;
	};

	public MediaDescription getMediaDescription() {
		return md;
	}

	public void setRtpSession(RtpSession session) {
		this.session = session;
	}

	public RtpSession getSession() {
		return session;
	}

}