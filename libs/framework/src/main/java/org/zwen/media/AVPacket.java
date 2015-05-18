package org.zwen.media;

import java.nio.ByteBuffer;

import javax.media.Buffer;

import org.apache.commons.lang.time.DateFormatUtils;

public class AVPacket extends Buffer {
	private int streamIndex;
	private long compositionTime = 0;
	private AVTimeUnit timeUnit;

	public AVPacket(AVStream av) {
		this.setFormat(av.getFormat());
		this.streamIndex = av.getStreamIndex();
		this.timeUnit = av.getTimeUnit();
	}


	public void setCompositionTime(long compositionTime) {
		this.compositionTime = compositionTime;
	}
	
	public long getCompositionTime(AVTimeUnit unit) {
		return unit.convert(compositionTime, timeUnit);
	}
	
	public void setTimeUnit(AVTimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public AVTimeUnit getTimeUnit() {
		return timeUnit;
	}



	public boolean isKeyFrame() {
		return (this.getFlags() & Buffer.FLAG_KEY_FRAME) > 0;
	}
	
	public void setKeyFrame(boolean isKey) {
		if (isKey) {
			this.setFlags(this.getFlags() | Buffer.FLAG_KEY_FRAME);
		} else {
			this.setFlags(this.getFlags() & ~Buffer.FLAG_KEY_FRAME);
		}
	}
	public void setEOM(boolean isEOM) {
		if (isEOM) {
			this.setFlags(this.getFlags() | Buffer.FLAG_EOM);
		} else {
			this.setFlags(this.getFlags() & ~Buffer.FLAG_EOM);
		}
	}
	
	public void setExtra(ByteBuffer buf) {
		super.setHeader(buf);
	}
	
	public ByteBuffer getExtra() {
		return (ByteBuffer)super.getHeader();
	}
	
	public ByteBuffer getByteBuffer() {
		Object rawData = super.getData();
		if (null == rawData) {
			return ByteBuffer.allocate(0);
		}

		if (rawData instanceof byte[]) {
			return ByteBuffer.wrap((byte[]) rawData, getOffset(), getLength());
		} else if (rawData instanceof ByteBuffer) {
			return (ByteBuffer) rawData;
		}
		
		throw new IllegalArgumentException("Can't convert bytebuffer");
	}

	public void setData(ByteBuffer data) {
		super.setData(data);
		super.setLength(data.limit() - data.position());
	}
	
	public void setData(byte[] data) {
		setData(data, 0, data.length);
	}

	public void setData(byte[] data, int offset, int length) {
		setData(ByteBuffer.wrap(data, offset, length));
	}

	public void setPts(long timestamp) {
		super.setTimeStamp(timestamp);
	}

	public void setPts(long timestamp, AVTimeUnit unit) {
		super.setTimeStamp(timeUnit.convert(timestamp, unit));
	}

	

	public long getPts() {
		return this.getTimeStamp();
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

	public long getDuration(AVTimeUnit unit) {
		return unit.convert(this.getDuration(), this.timeUnit);
	}

	public int getStreamIndex() {
		return streamIndex;
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
