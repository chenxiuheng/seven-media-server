package org.zwen.media;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AVStream {
	private static final Format FORMAT_UNKNOWN = new Format("UNKNOWN");

	private static final Logger logger = LoggerFactory.getLogger(AVStream.class);
	
	public static final int UNKNOWN = -1;
	private Format format = FORMAT_UNKNOWN;
	private int frameRate = UNKNOWN;
	protected AVStreamExtra extra;
	protected int streamIndex;
	
	final public AVStreamExtra getExtra(){
		 return extra;
	 }

	/* used for PTS sync */
	/** 
	 * the max PTS between audio and video
	 * 
	 * The stream must update lastClose
	 *   when its own PTS left 700ms then sysClock,
	 */
	protected AtomicLong sysClock;
	

	/**
	 * the PTS of last packet
	 */
	protected long lastClock;
	
	/***
	 * the increment of last two packets
	 */
	protected long diff;
	
	/***
	 * the PTS of last packet.
	 *    normal, next pts should be lastClock + diff
	 */
	protected long lastPts = UNKNOWN;
	
	public AVStream(AtomicLong sysClock, int streamIndex) {
		this.sysClock = sysClock;
		this.lastClock = sysClock.get();
		this.streamIndex = streamIndex;
	}

	public void syncTimestamp(AVPacket packet) {
		final long lastClock = this.lastClock;
		final long lastDiff = this.diff;
		long pts = packet.getPts(AVTimeUnit.MILLISECONDS);
		long duration = packet.getDuration(AVTimeUnit.MILLISECONDS);
		
		if (this.lastPts != UNKNOWN) {
			long diff = pts - this.lastPts;
			if (diff < 0 || diff > 700) {
				diff = lastDiff;
			} 

			if (Math.abs(sysClock.get() - (lastClock + diff)) < 700) {
				sysClock.set(Math.max(sysClock.get(), lastClock + diff));
				
				packet.setPts(lastClock + diff, AVTimeUnit.MILLISECONDS);
				this.diff = diff;
			} else {
				packet.setPts(sysClock.get(), AVTimeUnit.MILLISECONDS);
			}
		} else {
			packet.setPts(sysClock.get(), AVTimeUnit.MILLISECONDS);
			this.diff = getDefaultTimestampDifferent(duration);
		}
		
		this.lastPts = pts;
		this.lastClock = packet.getPts(AVTimeUnit.MILLISECONDS);
		
		if (logger.isDebugEnabled()) {
			logger.debug("set diff = {}, {}", diff, packet);
		}
	}

	public void setFormat(Format format) {
		this.format = format;
	}
	
	private long getDefaultTimestampDifferent(long defaultDiff) {
		long diff = defaultDiff;
		if (defaultDiff < 700 && defaultDiff > 0) {
			return diff;
		}

		if (format instanceof VideoFormat) {
			if (getFrameRate() > 0) {
				diff = 1000 / getFrameRate();
			} else {
				diff = 40; // fps = 25, default
			}
		} else if (format instanceof AudioFormat) {
			diff = 23; 
			if (format.getEncoding().equals(Constants.MP3)) {
				diff = 26; // a group samples  mp3 always be 26ms,
			}
		} else if (null == format) {
			throw new IllegalArgumentException("Unknown AVStream Format");
		}
		
		return diff;
	}
	
	public int getSampleRate() {
		return 0;
	}

	public int getStreamIndex() {
		return streamIndex;
	}
	
	public int getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}
	
	public boolean isAudio() {
		return format instanceof AudioFormat;
	}
	
	public boolean isVideo() {
		return format instanceof VideoFormat;
	}
	
	public static boolean hasVideo(List<AVStream> streams) {
		for (AVStream avStream : streams) {
			if (avStream.getFormat() instanceof VideoFormat) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean hasAudio(List<AVStream> streams) {
		for (AVStream avStream : streams) {
			if (avStream.getFormat() instanceof AudioFormat) {
				return true;
			}
		}
		
		return false;
	}
	
	public Format getFormat() {
		return format;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		
		buf.append("AVStream#").append(streamIndex);
		
		buf.append(" ");
		buf.append(format);
		
		if (lastPts != UNKNOWN) {
			buf.append(" last_pts=").append(DateFormatUtils.format(lastPts, "HH:mm:ss,SSS"));
		}
		
		return buf.toString();
	}
}
