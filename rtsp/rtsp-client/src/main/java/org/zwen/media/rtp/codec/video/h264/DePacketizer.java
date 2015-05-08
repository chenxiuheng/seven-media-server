package org.zwen.media.rtp.codec.video.h264;

import java.util.List;

import javax.media.Buffer;
import javax.media.format.VideoFormat;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.zwen.media.AVPacket;
import org.zwen.media.Constants;
import org.zwen.media.rtp.codec.IDePacketizer;

import com.biasedbit.efflux.packet.DataPacket;

/**
 * @author res
 * 
 */
public class DePacketizer implements IDePacketizer {
	public final static byte sync_bytes[] = { 0, 0, 0, 1 };
	
	private int nalType;
	private long timestamp;
	private ChannelBuffer frame = ChannelBuffers.buffer(256 * 1024);

	public DePacketizer() {
	}

	public void process(DataPacket packet, List<AVPacket> out) {
		ChannelBuffer payload = packet.getData();
		int nalUnitType;
		int nalRefIdc;
		boolean isStart, isEnd;

		long naluSize;
		long pos;
		if (payload.readableBytes() < 1) {
			return;
		}

		/*
		 * +---------------+ |0|1|2|3|4|5|6|7| +-+-+-+-+-+-+-+-+ |F|NRI| Type |
		 * +---------------+
		 * 
		 * F must be 0.
		 */
		nalRefIdc = (payload.getByte(0) & 0x60) >> 5;
		nalUnitType = payload.getByte(0) & 0x1f;

		switch (nalUnitType) {
		case 0:
		case 30:
		case 31:
			/* undefined */
			return;
		case 25:
			/* STAP-B Single-time aggregation packet 5.7.1 */
			/* 2 byte extra header for DON */
			/** Not supported */
			return;
		case 24:
			/**
			   Figure 7 presents an example of an RTP packet that contains an STAP-
			   A.  The STAP contains two single-time aggregation units, labeled as 1
			   and 2 in the figure.
			       0                   1                   2                   3
			       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
			      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |                          RTP Header                           |
			      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |STAP-A NAL HDR |         NALU 1 Size           | NALU 1 HDR    |
			      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |                         NALU 1 Data                           |
			      :                                                               :
			      +               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |               | NALU 2 Size                   | NALU 2 HDR    |
			      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |                         NALU 2 Data                           |
			      :                                                               :
			      |                               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      |                               :...OPTIONAL RTP padding        |
			      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
			      Figure 7.  An example of an RTP packet including an STAP-A and two
					 single-time aggregation units
			*/
			// unsupported
			return;
		case 26:
			/* MTAP16 Multi-time aggregation packet	5.7.2 */
			return;
		case 27:
			/* MTAP24 Multi-time aggregation packet	5.7.2 */
			return;
		case 28:
		case 29:
			/* FU-A	Fragmentation unit	 5.8 */
			/* FU-B	Fragmentation unit	 5.8 */


			/* +---------------+
			 * |0|1|2|3|4|5|6|7|
			 * +-+-+-+-+-+-+-+-+
			 * |S|E|R| Type	   |
			 * +---------------+
			 *
			 * R is reserved and always 0
			 */
			isStart = (payload.getByte(1) & 0x80) == 0x80;
			isEnd = (payload.getByte(1) & 0x40) == 0x40;

			/* strip off FU indicator and FU header bytes */
			naluSize = payload.readableBytes()-2;

			if (isStart)
			{
				resetFrame(out, false);
				
				/* NAL unit starts here */
				int nal_header;

				/* reconstruct NAL header */
				nal_header = (payload.getByte(0) & 0xe0) | (payload.getByte(1) & 0x1f);

				timestamp = packet.getTimestamp();
				frame.writeBytes(sync_bytes);
				frame.writeByte(nal_header);
				nalType = payload.getByte(1) & 0x1f;
			}
			
			payload.skipBytes(2);
			frame.writeBytes(payload);
			
			if (isEnd) {
				resetFrame(out, false);
			}
			break;
		default:
			
			break;
		}

	}

	private void resetFrame(List<AVPacket> out, boolean discard) {
		if (frame.readableBytes() > 0) {
			int length = frame.readableBytes();
			byte[] data = new byte[length];
			frame.readBytes(data);
			frame.clear();
			
			AVPacket buf = new AVPacket();
			buf.setDiscard(discard);
			buf.setData(data);
			buf.setDuration(40);
			buf.setFormat(new VideoFormat(Constants.H264_RTP));
			buf.setTimeStamp(timestamp);
			if (nalType == 5) {
				buf.setFlags(Buffer.FLAG_KEY_FRAME);
			}
			out.add(buf);
		}
	}

}
