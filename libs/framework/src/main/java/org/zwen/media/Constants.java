package org.zwen.media;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

public class Constants {

	public static final int MPEGTS_PACKET_SIZE = 188;
	public static String H264_RTP = "H264/RTP";
	public static String H264 = "H264";
	

	public static String AAC_RTP = "MPEG4-GENERIC";
	public static String AAC_ADTS = "AAC_ADTS";
	
	public static String MP3 = "mp3";
	
	public static class FORMATS {
		public static final AudioFormat AAC_ADTS = new AudioFormat(Constants.AAC_ADTS);
		public static final VideoFormat H264 = new VideoFormat(Constants.H264);
		public static final VideoFormat H264_RTP = new VideoFormat(Constants.H264_RTP);
	}
	
	
}
