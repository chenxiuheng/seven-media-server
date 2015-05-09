package org.zwen.media;


import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import javax.media.Buffer;
import javax.media.Format;

import org.apache.commons.lang3.time.DateFormatUtils;

public class AVPacket {
	private Buffer buffer;
	private AVTimeUnit timeunit;

	public AVPacket(AVTimeUnit unit) {
		this(new Buffer());
		this.timeunit = unit;
	}

	public AVPacket(Buffer buf) {
		this.buffer = buf;
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

	public void setDiscard(boolean discard) {
		buffer.setDiscard(discard);
	}
	
	public boolean isDiscard() {
		return buffer.isDiscard();
	}

	public void setData(byte[] data) {
		setData(data, 0, data.length);
	}

	public ByteBuffer getData() {
		if (null == buffer.getData()) {
			return null;
		}

		byte[] data = (byte[]) buffer.getData();
		return ByteBuffer.wrap(data, getOffset(), getLength());
	}

	public void setData(byte[] data, int offset, int length) {
		buffer.setData(data);
		buffer.setOffset(offset);
		buffer.setLength(length);
	}

	public void setOffset(int offset) {
		buffer.setOffset(offset);
	}

	public void setLength(int length) {
		buffer.setLength(length);
	}

	public void setTimeStamp(long timestamp) {
		buffer.setTimeStamp(timestamp);
	}
	
	public void setTimeStamp(long timestamp, AVTimeUnit unit) {
		buffer.setTimeStamp(timeunit.convert(timestamp, unit));
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

	public long getTimeStamp() {
		return buffer.getTimeStamp();
	}
	
	public long getTimeStamp(AVTimeUnit unit) {
		return unit.convert(getTimeStamp(), this.timeunit);
	}

	public int getLength() {
		return buffer.getLength();
	}

	public int getOffset() {
		return buffer.getOffset();
	}

	public long getDuration(AVTimeUnit unit) {
		return unit.convert(buffer.getDuration(), this.timeunit);
	}
	
	public AVTimeUnit getTimeunit() {
		return timeunit;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("AVPacket ");
		
		if (null != getFormat()) {
			buf.append(getFormat().getEncoding());
		} else {
			buf.append("UNKNOWN");
		}

		buf.append(", ");
		buf.append("key=").append(isKeyFrame()?"true ":"false");

		long ts = getTimeStamp(AVTimeUnit.MILLISECONDS);
		buf.append(", t=").append(DateFormatUtils.formatUTC(ts, "HH:mm:ss.SSS"));
		
		
		buf.append(", ");
		buf.append("size=").append(getLength());
		
		if (isDiscard()) {
			buf.append(" ");
			buf.append(" discard");
		}

		return buf.toString();
	}
}
