package org.zwen.media.core;

import java.util.List;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

public abstract class AVStream {
	private Format format;
	abstract public AVStreamExtra getExtra();

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
