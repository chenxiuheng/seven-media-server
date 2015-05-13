package org.zwen.media;

import java.nio.ByteBuffer;

import javax.media.Buffer;
import javax.media.Format;

import org.apache.commons.lang.time.DateFormatUtils;

public class AVPacket {
	private int streamIndex;
	private Buffer buffer;
	private long compositionTime = AVStream.UNKNOWN;
	private AVTimeUnit timeUnit;

	public AVPacket(AVStream stream) {
		this(new Buffer());
		
		setFormat(stream.getFormat());
		setStreamIndex(stream.getStreamIndex());
	}

	public AVPacket(Buffer buf) {
		this.buffer = buf;
	}

	public void setCompositionTime(long compositionTime) {
		this.compositionTime = compositionTime;
	}
	
	public void setTimeUnit(AVTimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public AVTimeUnit getTimeUnit() {
		return timeUnit;
	}

	public Format getFormat() {
		return buffer.getFormat();
	}

	public void setFormat(Format format) {
		buffer.setFormat(format);
	}

	public boolean isKeyFrame() {
		return (buffer.getFlags() & Buffer.FLAG_KEY_FRAME) > 0;
	}
	
	public void setKeyFrame(boolean isKey) {
		if (isKey) {
			buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
		} else {
			buffer.setFlags(buffer.getFlags() & ~Buffer.FLAG_KEY_FRAME);
		}
	}
	public void setEOM(boolean isEOM) {
		if (isEOM) {
			buffer.setFlags(buffer.getFlags() | Buffer.FLAG_EOM);
		} else {
			buffer.setFlags(buffer.getFlags() & ~Buffer.FLAG_EOM);
		}
	}
	
	public void setDiscard(boolean discard) {
		buffer.setDiscard(discard);
	}

	public boolean isDiscard() {
		return buffer.isDiscard();
	}

	public void setExtra(ByteBuffer buf) {
		buffer.setHeader(buf);
	}
	
	public ByteBuffer getExtra() {
		return (ByteBuffer)buffer.getHeader();
	}
	
	public ByteBuffer getData() {
		if (null == buffer.getData()) {
			return null;
		}

		ByteBuffer data = (ByteBuffer) buffer.getData();
		return data;
	}

	public void setData(ByteBuffer data) {
		this.buffer.setData(data);
		this.buffer.setLength(data.limit() - data.position());
	}
	
	public void setData(byte[] data) {
		setData(data, 0, data.length);
	}

	public void setData(byte[] data, int offset, int length) {
		setData(ByteBuffer.wrap(data, offset, length));
	}

	public void setPts(long timestamp) {
		buffer.setTimeStamp(timestamp);
	}

	public void setPts(long timestamp, AVTimeUnit unit) {
		buffer.setTimeStamp(timeUnit.convert(timestamp, unit));
	}

	public void setDuration(long duration) {
		buffer.setDuration(duration);
	}

	/**
	 * @param flag
	 * @see Buffer#setFlags(int)
	 */
	public void setFlags(int flag) {
		buffer.setFlags(flag);
	}

	public int getFlags() {
		return buffer.getFlags();
	}

	public long getPts() {
		return buffer.getTimeStamp();
	}
	
	public long getDts() {
		return compositionTime < 0 ? getPts() : getPts() + compositionTime;
	}
	
	public long getDts(AVTimeUnit unit) {
		return unit.convert(getDts(), this.timeUnit);
	}

	public long getPts(AVTimeUnit unit) {
		return unit.convert(getPts(), this.timeUnit);
	}

	public int getLength() {
		return buffer.getLength();
	}

	public int getOffset() {
		return buffer.getOffset();
	}

	public long getDuration() {
		return buffer.getDuration();
	}
	
	public long getDuration(AVTimeUnit unit) {
		return unit.convert(buffer.getDuration(), this.timeUnit);
	}

	public int getStreamIndex() {
		return streamIndex;
	}

	public void setStreamIndex(int streamIndex) {
		this.streamIndex = streamIndex;
	}

	public void setSequenceNumber(long packetIndex) {
		buffer.setSequenceNumber(packetIndex);
	}
	
	public long getSequenceNumber() {
		return buffer.getSequenceNumber();
	}

	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("AVPacket#");
		buf.append(streamIndex);
		buf.append(" ");
		


		if (null != getFormat()) {
			buf.append(getFormat().getEncoding());
		} else {
			buf.append("UNKNOWN");
		}
		
		buf.append(", ");
		buf.append("key=").append(isKeyFrame() ? "true " : "false");


		
		if (null != getTimeUnit()) {
			long ts = getPts(AVTimeUnit.MILLISECONDS);
			buf.append(", pts=").append(
					DateFormatUtils.formatUTC(ts, "HH:mm:ss,SSS"));
		} 

		
		buf.append(", ");
		buf.append("size=").append(getLength());

		buf.append(", ");
		buf.append("pos=").append(getSequenceNumber());
		
		buf.append(", t=").append(getPts());

		long duration = getDuration(AVTimeUnit.MILLISECONDS);
		if (getDuration(AVTimeUnit.MILLISECONDS) > 0){
			buf.append(", duration=").append(duration).append("ms");
		}
		
		if (isDiscard()) {
			buf.append(" ");
			buf.append(" discard");
		}

		return buf.toString();
	}


}
