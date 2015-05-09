package org.zwen.media;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AVStream {
	private static final Logger logger = LoggerFactory.getLogger(AVStream.class);
	
	public static final long UNKNOWN = -1;
	private Format format;
	abstract public AVStreamExtra getExtra();

	/* used for PTS sync */
	/** 
	 * the max PTS between audio and video
	 * 
	 * The stream must update lastClose
	 *   when its own PTS left 700ms then sysClock,
	 */
	protected AtomicLong sysClock = new AtomicLong(0);
	
	/***
	 * the increment of last two packets
	 */
	protected long diff;

	/***
	 * the PTS of last packet.
	 *    normal, next pts should be lastClock + diff
	 */
	protected long lastPts = UNKNOWN;
	
	protected AVStream(AtomicLong sysClock, Format format) {
		this.sysClock = sysClock;
		this.format = format;
	}

	public void syncTimestamp(AVPacket packet) {
		final long lastPts = this.lastPts;
		final long lastDiff = this.diff;
		long pts = packet.getTimeStamp(AVTimeUnit.MILLISECONDS);
		long duration = packet.getDuration(AVTimeUnit.MILLISECONDS);
		
		if (lastPts != UNKNOWN) {
			long diff = pts - lastPts;
			if (diff < 0 || diff > 700) {
				diff = lastDiff;
			} 

			long pktPts = lastPts + diff;
			if (Math.abs(sysClock.get() - pktPts) < 700) {
				sysClock.set(Math.max(sysClock.get(), pktPts));
			} else {
				pktPts = sysClock.get();
				diff = getDefaultTimestampDifferent(duration);
			}

			packet.setTimeStamp(pktPts, AVTimeUnit.MILLISECONDS);
			this.lastPts = pktPts;
			this.diff = diff;
		} else {
			packet.setTimeStamp(sysClock.get(), AVTimeUnit.MILLISECONDS);
			this.lastPts = sysClock.get();
			this.diff = getDefaultTimestampDifferent(duration);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("set pts = {}, diff = {}", DateFormatUtils.formatUTC(this.lastPts, "HH:mm:ss.SSS"), diff);
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
	
	public int getSampleRate() {
		return 0;
	}

	public int getFrameRate() {
		return 25;
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
}
