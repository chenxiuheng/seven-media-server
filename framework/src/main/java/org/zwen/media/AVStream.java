package org.zwen.media;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.codec.audio.aac.AACExtra;
import org.zwen.media.codec.video.h264.H264Extra;


public class AVStream {
	/** a group samples  mp3 always be 26ms, */
	private static final int MP3_DIFF = 26 * 90;
	/** experience value */
	private static final int MIN_AUDIO_DIFF = 23 * 90;
	/** 25 fps **/
	private static final int MIN_VIDEO_DIFF = 40 * 90;
	/** in MPEG TS, the diff is GT 700ms, Player must do sync */
	private static final int MAX_ASYNC_DIFF = 300 * 90;

	private static final Format FORMAT_UNKNOWN = new Format("UNKNOWN");

	private static final Logger logger = LoggerFactory.getLogger(AVStream.class);
	
	public static final int UNKNOWN = -1;

	protected int streamIndex;
	private double frameRate = UNKNOWN;
	private int sampleRate = UNKNOWN;
	private int numChannels = UNKNOWN;
	private AVTimeUnit timeUnit;

	private Format format = FORMAT_UNKNOWN;
	private AVStreamExtra extra;
	private int height = UNKNOWN;
	private int width = UNKNOWN;
	
	protected long max_async_diff = MAX_ASYNC_DIFF;
	
	
	

	/* used for PTS sync */
	/** 
	 * the max PTS between audio and video
	 * 
	 * The stream must update lastClose
	 *   when its own PTS left 700ms then sysClock,
	 */
	protected SystemClock sysClock;
	

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
	
	public AVStream(SystemClock sysClock, int streamIndex) {
		this.sysClock = sysClock;
		this.lastClock = sysClock.get();
		this.streamIndex = streamIndex;
	}

	public void syncTimestamp(AVPacket packet) {
		final long lastClock = this.lastClock;
		final long lastDiff = this.diff;
		long pts = packet.getTimestamp(AVTimeUnit.MILLISECONDS_90);
		long duration = packet.getDuration(AVTimeUnit.MILLISECONDS_90);
		
		if (this.lastPts != UNKNOWN) {
			long diff = pts - this.lastPts;
			if (diff < 0 || diff > max_async_diff) {
				diff = lastDiff;
			} 

			long timestamp = sysClock.get();
			if (!sysClock.isInitialed() || Math.abs(timestamp - (lastClock + diff)) < max_async_diff) {
				sysClock.update(timestamp, Math.max(timestamp, lastClock + diff));
				
				packet.setTimestamp(lastClock + diff, AVTimeUnit.MILLISECONDS_90);
				this.diff = diff;
			} else {
				packet.setTimestamp(sysClock.get(), AVTimeUnit.MILLISECONDS_90);
			}
		} else if (sysClock.isInitialed()){
			packet.setTimestamp(sysClock.get(), AVTimeUnit.MILLISECONDS_90);
			this.diff = getDefaultTimestampDifferent(duration);
		} else {
			this.diff = getDefaultTimestampDifferent(duration);
		}
		
		packet.setDuration(packet.getTimeUnit().convert(diff, AVTimeUnit.MILLISECONDS_90));
		
		this.lastPts = pts;
		this.lastClock = packet.getTimestamp(AVTimeUnit.MILLISECONDS_90);
		
		if (logger.isDebugEnabled()) {
			// logger.debug("set diff = {}, {}", diff, packet);
		}
	}
	private long getDefaultTimestampDifferent(long defaultDiff) {
		long diff = defaultDiff;
		if (defaultDiff < max_async_diff && defaultDiff > 0) {
			return diff;
		}

		if (format instanceof VideoFormat) {
			if (getFrameRate() > 0) {
				diff = (int)Math.ceil(1000 * 90 / getFrameRate());
			} else {
				diff = MIN_VIDEO_DIFF; // fps = 25, default
			}
		} else if (format instanceof AudioFormat) {
			diff = MIN_AUDIO_DIFF; 
			if (format.getEncoding().equals(Constants.MP3)) {
				diff = MP3_DIFF; // a group samples  mp3 always be 26ms,
			}
		} else if (null == format) {
			throw new IllegalArgumentException("Unknown AVStream Format");
		}
		
		return diff;
	}
	
	
	
	final public AVStreamExtra getExtra(){
		 return extra;
	 }
	
	public void setExtra(AVStreamExtra extra) {
		this.extra = extra;
		if (extra instanceof H264Extra) {
			H264Extra h264 = (H264Extra)extra;
			setWidth(h264.getWidth());
			setHeight(h264.getHeight());
		} else if (extra instanceof AACExtra) {
			AACExtra aac = (AACExtra)extra;
			this.numChannels = aac.getNumChannels();
			this.sampleRate = aac.getSampleRate();
		}
	}
	
	public int getNumChannels() {
		return numChannels;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}

	public void setFormat(Format format) {
		this.format = format;
	}
	
	
	public int getSampleRate() {
		return sampleRate;
	}

	public int getStreamIndex() {
		return streamIndex;
	}
	
	public double getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(double frameRate) {
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
	
	public void setTimeUnit(AVTimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}
	
	public AVTimeUnit getTimeUnit() {
		return timeUnit;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		
		buf.append("AVStream#").append(streamIndex);
		
		buf.append(" ");
		buf.append(null != format ? format.getEncoding() : "");
		
		if (lastPts != UNKNOWN) {
			buf.append(" last_pts=").append(DateFormatUtils.format(AVTimeUnit.MILLISECONDS.convert(lastPts, timeUnit), "HH:mm:ss,SSS"));
		}
		
		if (format instanceof VideoFormat) {
			buf.append(", s=").append(width).append("×").append(height);
		} else if (format instanceof AudioFormat) {
			buf.append(", sampleRate=").append(sampleRate);
			buf.append(", channels=").append(numChannels);
		}
		
		return buf.toString();
	}
}
