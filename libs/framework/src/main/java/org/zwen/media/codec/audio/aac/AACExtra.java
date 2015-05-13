package org.zwen.media.codec.audio.aac;

import org.jcodec.codecs.aac.Profile;
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
	public static Profile[] PROFILES = {
				Profile.MAIN, 
				Profile.LC, 
				Profile.OTHER, 
				Profile.OTHER
			};
	

	private int numChannels = 2;
	private int sampleRate = 44100; // 44100, is the only value supported by flv
	private Profile profile = Profile.LC;

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

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}
}
