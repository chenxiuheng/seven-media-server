package org.zwen.media.protocol.rtsp.sdp.audio.aac;

import org.zwen.media.AVStreamExtra;

public class AACExtra implements AVStreamExtra {

	public static final int[] AUDIO_SAMPLING_RATES = { 
			96000, // 0
			88200, // 1
			64000, // 2
			48000, // 3
			44100, // 4
			32000, // 5
			24000, // 6
			22050, // 7
			16000, // 8
			12000, // 9
			11025, // 10
			8000, // 11
			7350, // 12
			-1, // 13
			-1, // 14
			-1, // 15
	};

	private int numChannels;
	private int sampleRate; // pre channel
	private byte[] config;

	public void setConfig(byte[] config) {
		this.config = config;
	}

	public byte[] getConfig() {
		return config;
	}

	public int getNumChannels() {
		return numChannels;
	}

	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
}
