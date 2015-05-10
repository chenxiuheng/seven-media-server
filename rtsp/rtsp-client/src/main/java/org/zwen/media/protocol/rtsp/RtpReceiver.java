package org.zwen.media.protocol.rtsp;

import gov.nist.javax.sdp.fields.MediaField;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.Format;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVStreamDispatcher;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.protocol.rtsp.sdp.video.h264.FmtpValues;
import org.zwen.media.rtp.JitterBuffer;
import org.zwen.media.rtp.codec.IDePacketizer;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.DefaultRtpSession;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionDataListener;
import com.biasedbit.efflux.session.SingleParticipantSession;

public class RtpReceiver extends AVStream {

	private static final Logger logger = LoggerFactory
			.getLogger(RtpReceiver.class);
	private int payloadType;
	private int streamIndex;

	protected MediaDescription md;
	private RtpSession session;
	private IDePacketizer dePacketizer;
	private long lastNumSeq = 0;

	public RtpReceiver(AtomicLong sysClock, Format format, MediaDescription md,
			IDePacketizer dePacketizer) {
		super(sysClock, format);
		this.md = md;
		this.dePacketizer = dePacketizer;

		String rtpmap;
		payloadType = 0;
		try {
			rtpmap = md.getAttribute("rtpmap");

			if (null != rtpmap) {
				String strValue = StringUtil.split(rtpmap, ' ')[0];
				payloadType = Integer.valueOf(strValue);
			}
		} catch (SdpParseException e) {
			logger.error(e.getMessage(), e);
		}

		setTimeUnit(AVTimeUnit.MILLISECONDS);
	}

	public void setTimeUnit(AVTimeUnit unit) {
		super.setTimeUnit(unit);
		if (null != dePacketizer) {
			dePacketizer.setTimeUnit(unit);
		}
	}

	public void setStreamIndex(int streamIndex) {
		this.streamIndex = streamIndex;
	}

	public int getStreamIndex() {
		return streamIndex;
	}

	public int getPayloadType() {
		return payloadType;
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

	public MediaDescription getMediaDescription() {
		return md;
	}

	public boolean connect(String id, RtpParticipant localParticipant,
			RtpParticipant remoteParticipant,
			final AVStreamDispatcher dispatcher) {
		if (null != remoteParticipant) {
			session = new SingleParticipantSession(id, payloadType,
					localParticipant, remoteParticipant);
		} else {
			session = new DefaultRtpSession(id, payloadType, localParticipant);
		}

		final List<AVPacket> out = new ArrayList<AVPacket>(4);
		final JitterBuffer buffer = new JitterBuffer(isVideo() ? 64 : 4);

		if (null != dePacketizer) {
			session.addDataListener(new RtpSessionDataListener() {

				@Override
				public void dataPacketReceived(RtpSession session,
						RtpParticipantInfo participant, DataPacket packet) {
					DataPacket last = buffer.add(packet);

					if (null != last) {
						// check rtp lost?
						if (lastNumSeq + 1 != last.getSequenceNumber()) {
							logger.info("last data packet, except {} but {}", lastNumSeq + 1, last.getSequenceNumber());
						}
						lastNumSeq = last.getSequenceNumber();

						dePacketizer.process(last, out);

						while (!out.isEmpty()) {
							AVPacket pkt = out.remove(0);

							pkt.setStreamIndex(streamIndex);
							syncTimestamp(pkt);
							dispatcher.firePacket(RtpReceiver.this, pkt);
						}
					}
				}
			});
		}

		return session.init();
	}

	public RtpSession getSession() {
		return session;
	}

	public AVStreamExtra getExtra() {
		return null;
	}

}