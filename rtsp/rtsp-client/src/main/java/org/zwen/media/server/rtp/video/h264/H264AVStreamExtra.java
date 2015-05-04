package org.zwen.media.server.rtp.video.h264;

import org.zwen.media.core.AVStreamExtra;

public class H264AVStreamExtra implements AVStreamExtra {
	private byte[] profile;
	private byte[][] sps;
	private byte[][] pps;
	public byte[] getProfile() {
		return profile;
	}
	public void setProfile(byte[] profile) {
		this.profile = profile;
	}
	public byte[][] getSps() {
		return sps;
	}
	public void setSps(byte[][] sps) {
		this.sps = sps;
	}
	public byte[][] getPps() {
		return pps;
	}
	public void setPps(byte[][] pps) {
		this.pps = pps;
	}
}
