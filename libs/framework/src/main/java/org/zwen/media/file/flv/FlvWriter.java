package org.zwen.media.file.flv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jcodec.codecs.h264.H264Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.AVWriter;
import org.zwen.media.Constants;
import org.zwen.media.codec.audio.aac.AACExtra;
import org.zwen.media.codec.video.h264.H264Extra;

import com.flazr.rtmp.message.MetadataAmf0;

public class FlvWriter implements AVWriter {
	private static final Logger logger = LoggerFactory
			.getLogger(FlvWriter.class);

	private GatheringByteChannel out;


	private AVStream[] streams;
	
	public FlvWriter(GatheringByteChannel out) {
		this.out = out;
	}
	
	public void setStreams(AVStream[] streams) {
		this.streams = streams;
	}


	public void writeHead() throws IOException {
		if (null == streams) {
			throw new IllegalArgumentException("No Streams Found");
		}

		ChannelBuffer buffer = ChannelBuffers.buffer(1024);

		int dataSize = 0;

		// flv header
		buffer.writeByte('F');
		buffer.writeByte('L');
		buffer.writeByte('V');
		buffer.writeByte(0x01); // version
		buffer.writeByte(0x05); // audio & video
		buffer.writeInt(0x09); // header size

		buffer.writeInt(0x00); // first tag size

		// metadata
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("duration", 0);
		for (int i = 0; i < streams.length; i++) {
			AVStream av = (AVStream)streams[i];
			
			if (av.getFormat() instanceof VideoFormat) {
				if (av.getWidth() > 0) {
					map.put("width", av.getWidth());
				}
				if (av.getHeight() > 0) {
					map.put("heigth", av.getHeight());
				}
				if (av.getFrameRate() > 0) {
					map.put("videoframerate", av.getFrameRate());
				}
			} else if (av.getFormat() instanceof AudioFormat) {
				if (av.getSampleRate() > 0) {
					map.put("audiosamplerate", av.getSampleRate());
				}
				if (av.getNumChannels() > 0) {
					map.put("audiochannels", av.getNumChannels());
				}
			}
		}
		
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
			if (extra instanceof H264Extra) {
				H264Extra h264 = (H264Extra) extra;
				ByteBuffer profile = h264.readProfile();
				if (null == profile || profile.remaining() != 3) {
					logger.warn("H264.profile is NOT FOUND");
					continue;
				}

				ChannelBuffer avc = ChannelBuffers.buffer(128);
				avc.writeByte(0x17); // key frame + avc
				avc.writeByte(0x00); // avc sequence header
				avc.writeMedium(0x00); // Composition time offset

				
				avc.writeByte(0x01);
				avc.writeBytes(profile);
				avc.writeByte(0xFF); // 4 byte for nal header length

				// sps(s)
				ByteBuffer sps = h264.readSps();
				avc.writeByte(0xE0 | 0x01);
				avc.writeShort(sps.remaining());
				avc.writeBytes(sps);

				// pps(s)
				ByteBuffer pps = h264.readPps();
				avc.writeByte(0x01);
				avc.writeShort(pps.remaining());
				avc.writeBytes(pps);

				dataSize = avc.readableBytes();
				buffer.writeByte(0x09); // video type
				buffer.writeMedium(dataSize);
				buffer.writeInt(0); // timestamp
				buffer.writeMedium(0); // stream id
				buffer.writeBytes(avc);
				
				buffer.writeInt(11 + dataSize);
			} else if (extra instanceof AACExtra) {
				AACExtra aac = (AACExtra)extra;
				
				ChannelBuffer data = ChannelBuffers.buffer(4);
				
				data.writeByte(0xAF); // aac, 44100, 2 channels, stereo
				data.writeByte(0x00); // aac header
				
				dataSize = data.readableBytes();
				buffer.writeByte(0x08);
				buffer.writeMedium(data.readableBytes()); // data size
				buffer.writeInt(0x00); // timestamp
				buffer.writeMedium(0x00); // stream id
				
				buffer.writeBytes(data); // data

				buffer.writeInt(11 + dataSize); // pre tag size
			} else {
				logger.warn("unsupported {}", stream.getFormat());
			}
		}

		buffer.readBytes(out, buffer.readableBytes());
	}

	public void write(AVStream av, AVPacket frame) throws IOException {
		Format vf = frame.getFormat();
		if (Constants.H264.equals(vf.getEncoding())) {
			writeH264WithStartCode(frame);
			return;
		} else if (Constants.AAC.equalsIgnoreCase(vf.getEncoding())) {
			writeAAC(frame);
			return;
		}

		logger.warn("unsupported format {}", frame.getFormat());
	}

	private void writeH264WithStartCode(AVPacket frame) throws IOException {
		ByteBuffer data = frame.getData();
		if (data.remaining() > 4 && 1 != data.getInt(data.position())) {
			logger.warn("H264 Not Start With 0x00 00 00 01");
			return;
		}
		List<ByteBuffer> segs = H264Utils.splitFrame(data);
		
		int dataSize = 5 + 4 * segs.size();
		for (ByteBuffer seg : segs) {
			dataSize += seg.remaining();
		}

		ChannelBuffer avc = ChannelBuffers.buffer(11 + frame.getLength() + 4 + 4 + 5);
		avc.writeByte(0x09); // video type
		avc.writeMedium(dataSize); // tag data size
		
		// timestamp
		long timestamp = frame.getPts(AVTimeUnit.MILLISECONDS);
		avc.writeMedium((int)(0xFFFFFF & timestamp));
		avc.writeByte((int)(0xFF & (timestamp >> 24)));

		avc.writeMedium(0x0); // stream id
		
		avc.writeByte(frame.isKeyFrame() ? 0x27 : 0x17); // key frame + avc
		avc.writeByte(0x01); // avc NAL
		avc.writeMedium((int)frame.getCompositionTime(AVTimeUnit.MILLISECONDS)); // Composition time offset
		
		// data
		for (ByteBuffer seg : segs) {
			avc.writeInt(seg.remaining());
			avc.writeBytes(seg);
		}
		
		avc.writeInt(avc.readableBytes()); // pre tag size

		avc.readBytes(out, avc.readableBytes());
	}

	private void writeAAC(AVPacket frame) throws IOException {
		ByteBuffer data = frame.getData();
		int dataSize = 2 + data.remaining();
		
		ChannelBuffer aac = ChannelBuffers.buffer(11 + dataSize + 4);
		aac.writeByte(0x08);
		aac.writeMedium(dataSize);
		
		// timestamp
		long timestamp = frame.getPts(AVTimeUnit.MILLISECONDS);
		aac.writeMedium((int)(0xFFFFFF & timestamp));
		aac.writeByte((int)(0xFF & (timestamp >> 24)));
		
		aac.writeMedium(0);
		
		aac.writeByte(0xAF); // aac, 44100, 2 channels, stereo
		aac.writeByte(0x01); // aac data
		aac.writeBytes(data);
		
		aac.writeInt(aac.readableBytes()); // pre tag size
		aac.readBytes(out, aac.readableBytes());
	}

	@Override
	public void writeTail() throws IOException {
		
	}
	
	@Override
	public void close() throws IOException {
		if (null != out) {
			out.close();
		}
	}


}
