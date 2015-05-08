package org.zwen.media;


import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import javax.media.Buffer;
import javax.media.Format;

public class AVPacket {
	private Buffer buffer;

	public AVPacket() {
		this(new Buffer());
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
	
	public long getTimeStamp(TimeUnit unit) {
		return TimeUnit.MILLISECONDS.convert(getTimeStamp() / 90, unit);
	}

	public int getLength() {
		return buffer.getLength();
	}

	public int getOffset() {
		return buffer.getOffset();
	}

}
