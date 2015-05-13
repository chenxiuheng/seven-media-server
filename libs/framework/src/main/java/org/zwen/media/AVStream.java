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
	private static final Format FORMAT_UNKNOWN = new Format("UNKNOWN");

	private static final Logger logger = LoggerFactory.getLogger(AVStream.class);
	
	public static final int UNKNOWN = -1;

	protected int streamIndex;
	private int frameRate = UNKNOWN;
	private int sampleRate = UNKNOWN;
	private int numChannels = UNKNOWN;

	private Format format = FORMAT_UNKNOWN;
	protected AVStreamExtra extra;
	private int height = UNKNOWN;
	private int width = UNKNOWN;
	
	
	

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
		buf.append(null != format ? format.getEncoding() : "");
		
		if (lastPts != UNKNOWN) {
			buf.append(" last_pts=").append(DateFormatUtils.format(lastPts, "HH:mm:ss,SSS"));
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
