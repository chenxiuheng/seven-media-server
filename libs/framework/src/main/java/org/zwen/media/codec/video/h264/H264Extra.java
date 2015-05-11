package org.zwen.media.codec.video.h264;

import org.zwen.media.AVStreamExtra;

public class H264Extra implements AVStreamExtra {
	private int packetMode;
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
	
	public void setPacketMode(int packetMode) {
		this.packetMode = packetMode;
	}
	
	public int getPacketMode() {
		return packetMode;
	}
}
