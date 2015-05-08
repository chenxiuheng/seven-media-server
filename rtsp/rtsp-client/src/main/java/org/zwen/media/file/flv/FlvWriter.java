package org.zwen.media.file.flv;

import static com.flazr.rtmp.message.AbstractMessage.map;
import static com.flazr.rtmp.message.AbstractMessage.pair;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.media.format.VideoFormat;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.Constants;
import org.zwen.media.protocol.rtsp.sdp.video.h264.H264AVStreamExtra;

import com.flazr.rtmp.message.MetadataAmf0;

public class FlvWriter implements Closeable {
	private static final Logger logger = LoggerFactory
			.getLogger(FlvWriter.class);

	private GatheringByteChannel out;

	private List<AVStream> streams = Collections.emptyList();

	public FlvWriter(GatheringByteChannel out) {
		this.out = out;
	}

	public void setStreams(List<? extends AVStream> streams) {
		this.streams = new ArrayList<AVStream>(streams);
	}

	public void writeHead() throws IOException {
		ChannelBuffer buffer = ChannelBuffers.buffer(1024);

		int dataSize = 0;
		boolean hasVideo = AVStream.hasVideo(streams);
		boolean hasAudio = AVStream.hasAudio(streams);

		// flv header
		buffer.writeByte('F');
		buffer.writeByte('L');
		buffer.writeByte('V');
		buffer.writeByte(0x01); // version
		buffer.writeByte(0x05); // audio & video
		buffer.writeInt(0x09); // header size

		buffer.writeInt(0x00); // first tag size

		// metadata
		Map<String, Object> map = map(pair("duration", 0),
				pair("width", 640.0), pair("height", 352.0), pair(
						"videocodecid", "avc1"), pair("audiocodecid", "mp4a"),
				pair("avcprofile", 100.0), pair("avclevel", 30.0), pair(
						"aacaot", 2.0), pair("videoframerate",
						29.97002997002997), pair("audiosamplerate", 24000.0),
				pair("audiochannels", 2.0));
		ChannelBuffer onMetaData = new MetadataAmf0("onMetaData", map).encode();
		dataSize = onMetaData.readableBytes();
		buffer.writeByte(0x18); // script type
		buffer.writeMedium(dataSize);
		buffer.writeInt(0); // timestamp
		buffer.writeMedium(0); // stream id
		buffer.writeBytes(onMetaData);

		buffer.writeInt(dataSize + 11); // pre tag size

		// stream config(s)
		for (AVStream stream : streams) {
			AVStreamExtra extra = stream.getExtra();
			if (extra instanceof H264AVStreamExtra) {
				H264AVStreamExtra h264 = (H264AVStreamExtra) extra;
				
				ChannelBuffer avc = ChannelBuffers.buffer(128);
				avc.writeByte(0x17); // key frame + avc
				avc.writeByte(0x00); // avc sequence header
				avc.writeMedium(0x00); // Composition time offset

				if (null == h264.getProfile() || h264.getProfile().length != 3) {
					throw new IllegalArgumentException(
							"illegal profile of h264 - "
									+ Arrays.toString(h264.getProfile()));
				}
				avc.writeByte(0x01);
				avc.writeBytes(h264.getProfile());
				avc.writeByte(0xFF); // 4 byte for nal header length

				// sps(s)
				avc.writeByte(0xE0 | h264.getSps().length);
				for (int i = 0; i < h264.getSps().length; i++) {
					byte[] sps = h264.getSps()[i];
					avc.writeShort(sps.length);
					avc.writeBytes(sps);
				}

				// pps(s)
				avc.writeByte(h264.getPps().length);
				for (int i = 0; i < h264.getPps().length; i++) {
					byte[] pps = h264.getPps()[i];
					avc.writeShort(pps.length);
					avc.writeBytes(pps);
				}

				dataSize = avc.readableBytes();
				buffer.writeByte(0x09); // video type
				buffer.writeMedium(dataSize);
				buffer.writeInt(0); // timestamp
				buffer.writeMedium(0); // stream id
				buffer.writeBytes(avc);
				
				buffer.writeInt(11 + dataSize);
			} else {
				logger.warn("unsupported {}", stream.getFormat());
			}
		}

		buffer.readBytes(out, buffer.readableBytes());
	}

	public void write(AVPacket frame) throws IOException {
		if (frame.getFormat() instanceof VideoFormat) {
			VideoFormat vf = (VideoFormat) frame.getFormat();
			if (Constants.H264_RTP.equals(vf)) {
				writeH264WithStartCode(frame);
				return;
			}
		}

		logger.warn("unsupported format {}", frame.getFormat());
	}

	private void writeH264WithStartCode(AVPacket frame) throws IOException {
		ChannelBuffer data = ChannelBuffers.wrappedBuffer(frame.getData());


		ChannelBuffer avc = ChannelBuffers.buffer(11 + frame.getLength() + 4 + 4 + 5);
		avc.writeByte(0x09); // video type
		avc.writeMedium(0); // tag data size
		
		// timestamp
		long timestamp = frame.getTimeStamp(TimeUnit.MILLISECONDS);
		avc.writeMedium((int)(0xFFFFFF & timestamp));
		avc.writeByte((int)(0xFF & (timestamp >> 24)));
		
		avc.writeMedium(0x0); // stream id
		
		
		avc.writeByte(frame.isKeyFrame() ? 0x27 : 0x17); // key frame + avc
		avc.writeByte(0x01); // avc NAL
		avc.writeMedium(0xc8); // Composition time offset
		if (data.getMedium(0) == 1) {
			data.readMedium();
			avc.writeInt(data.readableBytes());
			avc.writeBytes(data);
		} else if (data.getInt(0) == 1) {
			data.readInt();
			avc.writeInt(data.readableBytes());
			avc.writeBytes(data);
		} else {
			logger.warn("Cant Find Start Code");
			return;
		}

		
		int tagSize = avc.readableBytes();
		int dataSize = tagSize - 11;
		avc.setMedium(1, dataSize); // update tag data size 
		avc.writeInt(tagSize);
		
		avc.readBytes(out, avc.readableBytes());
	}

	@Override
	public void close() throws IOException {
		if (null != out) {
			out.close();
		}
	}

}
