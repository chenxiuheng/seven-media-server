package org.zwen.media.protocol.rtsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.SessionAddress;

import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;
import jlibrtp.RtpPkt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVDispatcher;
import org.zwen.media.AVStream;
import org.zwen.media.SystemClock;
import org.zwen.media.rtp.codec.AbstractDePacketizer;

import com.biasedbit.efflux.session.RtpSession;

public class RtpDataSource extends AVStream implements RTPAppIntf {

	private static final Logger logger = LoggerFactory
			.getLogger(RtpDataSource.class);

	private String mediaType;

	private int payloadType;

	/** a=control:rtsp://127.0.0.1:8554/trackID=0 */
	private String controlUrl;

	/**
	 * <h4>SDP<h4>
	 * 
	 * a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr;
	 * config=139056e5a54800; SizeLength=13; IndexLength=3; IndexDeltaLength=3;
	 * Profile=1; or a=fmtp:96 packetization-mode=1;profile-level-id=
	 * 64001e;sprop-parameter-sets=Z2QAHqzIYCoMfkwEQAAAAwBAAAAMo8WLZ4A=,aOm7LIs=
	 * ;
	 * **/
	private String fmtpValue;

	// RTP-Info=url=rtsp://127.0.0.1:8554/trackID=0;seq=10268;rtptime=5875310,
	// url=rtsp://127.0.0.1:8554/trackID=1;seq=15231;rtptime=23980860
	private long seq = UNKNOWN;
	private long rtpTime = UNKNOWN;

	// Range=npt=7.143000-
	private long npt;

	private RtpSession session;
	private AbstractDePacketizer dePacketizer;

	public RtpDataSource(int streamIndex, SystemClock sysClock) {
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

	public boolean connect(String id, int ssrcid, SessionAddress local,
			SessionAddress remote, final AVDispatcher dispatcher)
			throws InvalidSessionAddressException, UnknownHostException,
			SocketException, IOException {
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;

		try {
			rtpSocket = new DatagramSocket(local.getDataPort());
			rtcpSocket = new DatagramSocket(local.getControlPort());
			RTPSession rtpSession = new RTPSession(rtpSocket, rtcpSocket);
			rtpSession.addParticipant(new Participant(remote.getDataAddress()
					.getHostAddress(), remote.getDataPort(), remote
					.getControlPort()));
			
			
			// send check rtp packet for NAT, must TWO rtp for check we meet Symmetric NAT or NOT
			byte[] buf = "abcdef".getBytes("UTF-8");
			rtpSocket.send(new DatagramPacket(buf, buf.length, new InetSocketAddress(remote.getDataAddress(), remote.getDataPort())));
			rtpSocket.send(new DatagramPacket(buf, buf.length, new InetSocketAddress(remote.getDataAddress(), remote.getDataPort())));
			
			// send rtcp 
			ByteBuffer rtcpRR = ByteBuffer.allocate(32);
			rtcpRR.putInt(0x80c90001); // receive port
			rtcpRR.putInt(ssrcid);
			rtcpRR.putInt(0x81ca0005); // source description
			rtcpRR.putInt(ssrcid);
			rtcpRR.put((byte)0x01);
			buf = "res@0.0.0.0".getBytes();
			rtcpRR.put((byte)buf.length);
			rtcpRR.put(buf);
			rtcpRR.put((byte)0x00);
			buf = rtcpRR.array();
			rtcpSocket.send(new DatagramPacket(buf, buf.length, new InetSocketAddress(remote.getDataAddress(), remote.getControlPort())));
			rtcpSocket.send(new DatagramPacket(buf, buf.length, new InetSocketAddress(remote.getDataAddress(), remote.getControlPort())));
			

			rtpSession.RTPSessionRegister(this, null, null);
			return true;
		} catch (Exception e) {
			closeQuietly(rtpSocket);
			closeQuietly(rtcpSocket);
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	private void closeQuietly(DatagramSocket socket) {
		if (null != socket) {
			socket.close();
		}
	}

	@Override
	public void userEvent(int type, Participant[] participant) {
		System.out.println(type);
	}

	@Override
	public void receiveData(RtpPkt frame, Participant participant) {
		logger.debug("{}", frame);
	}

	@Override
	public int bufferSize(int payloadType) {
		if ("video".equalsIgnoreCase(mediaType)) {
			return 1000;
		} else if ("audio".endsWith(mediaType)) {
			return 4;
		} else if (payloadType < 24) {
			return 2;
		} else if (payloadType < 128) {
			return 400;
		} else {
			return 4;
		}
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

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
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