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
import org.zwen.media.AVDispatcher;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.Constants;
import org.zwen.media.SystemClock;
import org.zwen.media.URLUtils;
import org.zwen.media.rtp.JitterBuffer;
import org.zwen.media.rtp.codec.AbstractDePacketizer;
import org.zwen.media.rtp.codec.audio.aac.Mpeg4GenericCodec;
import org.zwen.media.rtp.codec.video.h264.H264DePacketizer;

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
	
	private String controlUrl;
	private RtpSession session;
	private AbstractDePacketizer dePacketizer;
	private long lastNumSeq = 0;
	private AtomicLong pktCounter;


	private long rtpTime = UNKNOWN;
	
	public RtpReceiver(int streamIndex, SystemClock sysClock, AtomicLong pktCounter) {
		super(sysClock, streamIndex);
		this.pktCounter = pktCounter;
	}
	
	public void setControlUrl(String url) {
		this.controlUrl = url;
	}
	public String getControlUrl() {
		return controlUrl;
	}
	

	public boolean setMediaDescription(String baseUrl, MediaDescription md) throws SdpException {
		String control = md.getAttribute("control");
		this.controlUrl = URLUtils.getAbsoluteUrl(baseUrl, control);
		
		String mediaType = md.getMedia().getMediaType();
		for (Object item : md.getMedia().getMediaFormats(true)) {
			String format = (String) item;
			String rtpmap = md.getAttribute("rtpmap");
			String fmtp = md.getAttribute("fmtp");

			if (null == rtpmap || !rtpmap.startsWith(format)) {
				logger.warn("rtpmap is NULL for {}", md.getMedia());
				continue;
			}

			// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
			Matcher rtpMapParams = Pattern.compile(
					"(\\d+) ([^/]+)(/(\\d+)(/([^/]+))?)?(.*)?").matcher(rtpmap);
			if (!rtpMapParams.matches()) {
				logger.warn("{} is NOT legal rtpmap", rtpmap);
				return false;
			} else {
				setTimeUnit(AVTimeUnit.valueOf(Integer.valueOf(rtpMapParams.group(4))));
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
				this.dePacketizer = new H264DePacketizer();
				
				// framerate must gt 15 fps
				this.max_async_diff = getTimeUnit().convert(66, AVTimeUnit.MILLISECONDS);
			} else if ("MP4V-ES".equalsIgnoreCase(encoding)
					|| "mpeg4-generic".equalsIgnoreCase(encoding)
					|| "enc-mpeg4-generic".equalsIgnoreCase(encoding)
					|| "enc-generic-mp4".equalsIgnoreCase(encoding)) {
				setFormat(new AudioFormat(Constants.AAC));
				this.dePacketizer = new Mpeg4GenericCodec();
				this.max_async_diff = getTimeUnit().convert(100, AVTimeUnit.MILLISECONDS);
			}
			
			
			// read stream's extra info
			if (null != this.dePacketizer) {
				dePacketizer.setMediaDescription(this, md);
			}
		}
		
		return true;
	}


	public int getPayloadType() {
		return payloadType;
	}

	public boolean connect(String id, RtpParticipant localParticipant,
			RtpParticipant remoteParticipant,
			final AVDispatcher dispatcher) {
		if (null != remoteParticipant) {
			session = new SingleParticipantSession(id, payloadType,
					localParticipant, remoteParticipant);
		} else {
			session = new DefaultRtpSession(id, payloadType, localParticipant);
		}

		final List<AVPacket> out = new ArrayList<AVPacket>(4);
		final JitterBuffer buffer = new JitterBuffer(isVideo() ? 1 : 1);

		if (null != dePacketizer) {
			session.addDataListener(new RtpSessionDataListener() {

				@Override
				public void dataPacketReceived(RtpSession session,
						RtpParticipantInfo participant, DataPacket packet) {
					if (rtpTime == UNKNOWN) {
						logger.warn("ignore {}, pts = {}", packet.getPayloadType(), packet.getTimestamp());
						return;
					}
					DataPacket last = buffer.add(packet);

					if (null != last) {
						last.setTimestamp(last.getTimestamp() - rtpTime);

						// check rtp lost?
						if (lastNumSeq + 1 != last.getSequenceNumber()) {
							logger.info("stream#{} last data packet, except {} but {}", new Object[]{
									streamIndex, lastNumSeq + 1, last.getSequenceNumber()});
						}
						lastNumSeq = last.getSequenceNumber();

						dePacketizer.depacket(RtpReceiver.this, last, out);

						while (!out.isEmpty()) {
							AVPacket pkt = out.remove(0);
							
							pkt.setSequenceNumber(pktCounter.getAndIncrement());
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

	public void setRtpTime(long rtptime) {
		this.rtpTime = rtptime;
	}

}