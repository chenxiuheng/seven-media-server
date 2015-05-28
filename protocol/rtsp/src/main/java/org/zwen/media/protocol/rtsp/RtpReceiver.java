package org.zwen.media.protocol.rtsp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVDispatcher;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.SystemClock;
import org.zwen.media.rtp.JitterBuffer;
import org.zwen.media.rtp.codec.AbstractDePacketizer;

import com.biasedbit.efflux.packet.AppDataPacket;
import com.biasedbit.efflux.packet.CompoundControlPacket;
import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.DefaultRtpSession;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionControlListener;
import com.biasedbit.efflux.session.RtpSessionDataListener;
import com.biasedbit.efflux.session.SingleParticipantSession;

public class RtpReceiver extends AVStream {

	private static final Logger logger = LoggerFactory
			.getLogger(RtpReceiver.class);
	
	private int payloadType;
	
	/** a=control:rtsp://127.0.0.1:8554/trackID=0 */
	private String controlUrl;
	
	/**
	 * <h4>SDP<h4>
	 * 
	 *   a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=139056e5a54800; SizeLength=13; IndexLength=3; IndexDeltaLength=3; Profile=1;
	 * or
	 *   a=fmtp:96 packetization-mode=1;profile-level-id=64001e;sprop-parameter-sets=Z2QAHqzIYCoMfkwEQAAAAwBAAAAMo8WLZ4A=,aOm7LIs=;
	 * **/
	private String fmtpValue;
	
	// RTP-Info=url=rtsp://127.0.0.1:8554/trackID=0;seq=10268;rtptime=5875310, url=rtsp://127.0.0.1:8554/trackID=1;seq=15231;rtptime=23980860
	private long seq = UNKNOWN;
	private long rtpTime = UNKNOWN;
	
	// Range=npt=7.143000-
	private long npt;
	
	private RtpSession session;
	private AbstractDePacketizer dePacketizer;


	public RtpReceiver(int streamIndex, SystemClock sysClock) {
		super(sysClock, streamIndex);
	}

	public void setControlUrl(String url) {
		this.controlUrl = url;
	}

	public String getControlUrl() {
		return controlUrl;
	}


	public int getPayloadType() {
		return payloadType;
	}

	public boolean connect(String id, RtpParticipant localParticipant,
			RtpParticipant remoteParticipant, final AVDispatcher dispatcher) {
		if (null != remoteParticipant) {
			session = new SingleParticipantSession(id, payloadType,
					localParticipant, remoteParticipant);
		} else {
			session = new DefaultRtpSession(id, payloadType, localParticipant);
		}


		if (null != dePacketizer) {
			RtpSessionDataListener dataListener = makeDataListener(dispatcher);
			session.addDataListener(dataListener);
		}

		return session.init();
	}

	private RtpSessionDataListener makeDataListener(
			final AVDispatcher dispatcher) {
		RtpSessionDataListener dataListener = new RtpSessionDataListener() {
			final List<AVPacket> out = new ArrayList<AVPacket>(4);
			final JitterBuffer buffer = new JitterBuffer(isVideo() ? 64 : 4);
			
			@Override
			public void dataPacketReceived(RtpSession session,
					RtpParticipantInfo participant, DataPacket packet) {
				buffer.put(packet);
				DataPacket pkt = null;
				while (null != (pkt = buffer.tryGetNext(seq))) {
					pkt.setTimestamp(pkt.getTimestamp() - rtpTime);

					// check rtp lost?
					if (seq != pkt.getSequenceNumber()) {
						logger.info("last data packet, except {} but {}",
										seq, pkt.getSequenceNumber());
					} else {
						logger.debug("data packet[{}]",
										pkt.getSequenceNumber());
					}
					seq = (pkt.getSequenceNumber() + 1);
					if (seq > 65535) {
						seq = 0;
					}

					dePacketizer.depacket(RtpReceiver.this, pkt, out);

					while (!out.isEmpty()) {
						AVPacket frame = out.remove(0);

						// syncTimestamp(pkt);
						dispatcher.firePacket(RtpReceiver.this, frame);
					}
				}

			}
		};
		return dataListener;
	}

	public RtpSession getSession() {
		return session;
	}

	public void setRtpTime(long rtptime) {
		this.rtpTime = rtptime;
	}
	
	public void setSeq(long seq) {
		this.seq = seq;
	}

	public void setDePacketizer(AbstractDePacketizer dePacketizer) {
		this.dePacketizer = dePacketizer;
	}
	
	public AbstractDePacketizer getDePacketizer() {
		return dePacketizer;
	}
	public void setPayloadType(int payloadType) {
		this.payloadType = payloadType;
	}
}