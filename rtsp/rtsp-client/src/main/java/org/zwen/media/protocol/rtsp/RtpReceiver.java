package org.zwen.media.protocol.rtsp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVStreamDispatcher;
import org.zwen.media.rtp.JitterBuffer;
import org.zwen.media.rtp.codec.IDePacketizer;
import org.zwen.media.rtp.codec.video.h264.DePacketizer;

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
	
	private RtpSession session;
	private IDePacketizer dePacketizer;
	private long lastNumSeq = 0;
	private AtomicLong pktCounter;

	public RtpReceiver(AtomicLong sysClock, AtomicLong pktCounter) {
		super(sysClock);
		this.pktCounter = pktCounter;
	}

	public boolean setMediaDescription(MediaDescription md) throws SdpException {
		String mediaType = md.getMedia().getMediaType();
		RtpReceiver stream = null;
		for (Object item : md.getMedia().getMediaFormats(true)) {
			stream = null;
			String format = (String) item;
			String rtpmap = md.getAttribute("rtpmap");
			String fmtp = md.getAttribute("fmtp");

			if (null == rtpmap || !rtpmap.startsWith(format)) {
				logger.warn("rtpmap is NULL for {}", md.getMedia());
				continue;
			}

			Matcher rtpMapParams = Pattern.compile(
					"(\\d+) ([^/]+)(/(\\d+)(/([^/]+))?)?(.*)?").matcher(rtpmap);
			if (!rtpMapParams.matches()) {
				logger.warn("{} is NOT legal rtpmap", rtpmap);
				return false;
			}

			// payload type
			this.payloadType = Integer.valueOf(rtpMapParams.group(1));

			// media format
			String encoding = rtpMapParams.group(2);
			if ("audio".equalsIgnoreCase(mediaType)) {
				setFormat(new AudioFormat(encoding));
			} else if ("video".equalsIgnoreCase(mediaType)) {
				setFormat(new VideoFormat(encoding));
			}

			// payload DePacketizer
			if ("H264".equalsIgnoreCase(encoding)) {
				this.dePacketizer = new DePacketizer();
			} else if ("MP4V-ES".equalsIgnoreCase(encoding)
					|| "mpeg4-generic".equalsIgnoreCase(encoding)
					|| "enc-mpeg4-generic".equalsIgnoreCase(encoding)
					|| "enc-generic-mp4".equalsIgnoreCase(encoding)) {
				this.dePacketizer = new org.zwen.media.rtp.codec.audio.aac.DePacketizer();
			}
			
			
			// read stream's extra info
			if (null != this.dePacketizer) {
				this.extra = this.dePacketizer.depacketize(md);
			}
		}
		
		return true;
	}

	public void setStreamIndex(int streamIndex) {
		this.streamIndex = streamIndex;
	}

	public int getPayloadType() {
		return payloadType;
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
							logger.info("last data packet, except {} but {}",
									lastNumSeq + 1, last.getSequenceNumber());
						}
						lastNumSeq = last.getSequenceNumber();

						dePacketizer.process(last, out);

						while (!out.isEmpty()) {
							AVPacket pkt = out.remove(0);
							
							pkt.setPosition(pktCounter.getAndIncrement());
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

}