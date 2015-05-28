/*
 * This file is part of TinySip. 
 * http://code.google.com/p/de-tiny-sip/
 * 
 * Created 2011 by Sebastian R�sch <flowfire@sebastianroesch.de>
 * 
 * This software is licensed under the Apache License 2.0.
 */

package org.zwen.media.protocol.sip;

import gov.nist.javax.sip.address.SipUri;

import java.text.ParseException;
import java.util.List;

import javax.sip.PeerUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.ToHeader;

/**
 * Represents the local user's profile, containing his user name, the sip domain and so on. Also contains information about user authorization with the sip provider.
 * 
 * @author Sebastian
 * 
 */
public class LocalSipProfile {
	private String userName;
	private String displayName;
	private String sipDomain;
	private String sipPassword;
	private int sipPort;

	private int localSipPort;
	private int localAudioRtpPort;
	private int localAudioRtcpPort;
	private int localVideoRtpPort;
	private int localVideoRtcpPort;
	private List<SipAudioFormat> audioFormats;
	private List<SipVideoFormat> videoFormats;
	public boolean audio = true;
	public boolean video = false;

	private String nonce;
	private String realm;

	private String tag;


	/**
	 * Create a javax.sip.header.FromHeader for this LocalSipProfile
	 * 
	 * @param addressFactory
	 *            an instance of an AddressFactory, created by the SipManager
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @return the FromHeader for this LocalSipProfile
	 * @throws PeerUnavailableException
	 * @throws ParseException
	 */
	public FromHeader getFromHeader(AddressFactory addressFactory, HeaderFactory headerFactory) throws PeerUnavailableException, ParseException {
		SipURI fromAddress = addressFactory.createSipURI(this.userName, this.sipDomain);
		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(this.displayName);
		return headerFactory.createFromHeader(fromNameAddress, this.tag);
	}

	/**
	 * Create a javax.sip.header.ToHeader for this LocalSipProfile
	 * 
	 * @param addressFactory
	 *            an instance of an AddressFactory, created by the SipManager
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @return the ToHeader created for this LocalSipProfile
	 * @throws PeerUnavailableException
	 * @throws ParseException
	 */
	public ToHeader getToHeader(AddressFactory addressFactory, HeaderFactory headerFactory) throws PeerUnavailableException, ParseException {
		SipURI fromAddress = addressFactory.createSipURI(this.userName, this.sipDomain);
		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(this.displayName);
		return headerFactory.createToHeader(fromNameAddress, null);
	}

	/**
	 * Create a javax.sip.header.AuthorizationHeader for this LocalSipProfile
	 * 
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @return the AuthorizationHeader created for this LocalSipProfile
	 * @throws ParseException
	 */
	public AuthorizationHeader getAuthorizationHeader(HeaderFactory headerFactory) throws ParseException {
		SipUri uri = new SipUri();
		uri.setHost(this.sipDomain);

		String responseDigest = AuthorizationDigest.getDigest(this.userName, this.realm, this.sipPassword, "REGISTER", uri.toString(), this.nonce);

		AuthorizationHeader auth = headerFactory.createAuthorizationHeader("Digest");
		auth.setAlgorithm("MD5");
		auth.setNonce(this.nonce);
		auth.setRealm(this.realm);
		auth.setUsername(this.userName);
		auth.setURI(uri);
		auth.setResponse(responseDigest);

		return auth;
	}

	/**
	 * Create a javax.sip.header.ProxyAuthorizationHeader for this LocalSipProfile
	 * 
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @param contact
	 *            the SipContact to call
	 * @return the ProxyAuthorizationHeader for this LocalSipProfile
	 * @throws ParseException
	 */
	public ProxyAuthorizationHeader getProxyAuthorizationHeader(HeaderFactory headerFactory) throws ParseException {
		SipUri uri = new SipUri();
		uri.setHost(this.sipDomain);

		String responseDigest = AuthorizationDigest.getDigest(this.userName, this.realm, this.sipPassword, "INVITE", uri.toString(), this.nonce);

		ProxyAuthorizationHeader auth = headerFactory.createProxyAuthorizationHeader("Digest");
		auth.setAlgorithm("MD5");
		auth.setNonce(this.nonce);
		auth.setRealm(this.realm);
		auth.setUsername(this.userName);
		auth.setURI(uri);
		auth.setResponse(responseDigest);

		return auth;
	}

	/**
	 * Set the supported audio formats as a List of SipAudioFormat
	 * 
	 * @param audioFormats
	 *            the supported audio formats as a List of SipAudioFormat
	 */
	public void setAudioFormats(List<SipAudioFormat> audioFormats) {
		this.audioFormats = audioFormats;
	}

	/**
	 * @return the supported audio formats as a List of SipAudioFormat
	 */
	public List<SipAudioFormat> getAudioFormats() {
		return audioFormats;
	}

	/**
	 * Set the supported video formats as a List of SipVideoFormat
	 * 
	 * @param videoFormats
	 *            the supported audio formats as a List of SipVideoFormat
	 */
	public void setVideoFormats(List<SipVideoFormat> videoFormats) {
		this.videoFormats = videoFormats;
	}

	/**
	 * @return the supported video formats as a List of SipVideoFormat
	 */
	public List<SipVideoFormat> getVideoFormats() {
		return videoFormats;
	}

	/**
	 * Set the nonce to use for the authentication
	 * 
	 * @param nonce
	 *            the nonce to use for authentication
	 */
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	/**
	 * Set the realm to use for the authentication
	 * 
	 * @param realm
	 *            the realm to use for authentication
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * @return the profile's display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return the sip domain
	 */
	public String getSipDomain() {
		return sipDomain;
	}

	/**
	 * @return the sip password
	 */
	public String getSipPassword() {
		return sipPassword;
	}

	/**
	 * @return the local sip port
	 */
	public int getSipPort() {
		return sipPort;
	}

	/**
	 * @return the user name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Set the local rtp port for audio.
	 * 
	 * @param localAudioRtpPort
	 */
	public void setLocalAudioRtpPort(int localAudioRtpPort) {
		this.localAudioRtpPort = localAudioRtpPort;
	}

	/**
	 * @return the local rtp port for audio
	 */
	public int getLocalAudioRtpPort() {
		return localAudioRtpPort;
	}

	/**
	 * Set the local rtcp port for audio.
	 * 
	 * @param localAudioRtcpPort
	 */
	public void setLocalAudioRtcpPort(int localAudioRtcpPort) {
		this.localAudioRtcpPort = localAudioRtcpPort;
	}

	/**
	 * @return the local rtcp port for audio
	 */
	public int getLocalAudioRtcpPort() {
		return localAudioRtcpPort;
	}

	/**
	 * Set the local rtp port for video.
	 * 
	 * @param localVideoRtpPort
	 */
	public void setLocalVideoRtpPort(int localVideoRtpPort) {
		this.localVideoRtpPort = localVideoRtpPort;
	}

	/**
	 * @return the local rtp port for video
	 */
	public int getLocalVideoRtpPort() {
		return localVideoRtpPort;
	}

	/**
	 * Set the local rtcp port for video.
	 * 
	 * @param localVideoRtcpPort
	 */
	public void setLocalVideoRtcpPort(int localVideoRtcpPort) {
		this.localVideoRtcpPort = localVideoRtcpPort;
	}

	/**
	 * @return the local rtcp port for video
	 */
	public int getLocalVideoRtcpPort() {
		return localVideoRtcpPort;
	}

	/**
	 * @return the local sip port
	 */
	public int getLocalSipPort() {
		return localSipPort;
	}

	/**
	 * Set the local sip port
	 * 
	 * @param localSipPort
	 */
	public void setLocalSipPort(int localSipPort) {
		this.localSipPort = localSipPort;
	}
	
	/**
	 * @return the SipUri of the local sip profile
	 */
	public SipUri getSipUri(){
		SipUri uri = new SipUri();
		try {
			uri.setHost(this.sipDomain);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		uri.setUser(this.userName);
		if(isLocalProfile())
			uri.setPort(this.localSipPort);
		return uri;
	}

	/**
	 * @return whether this profile is a local profile only
	 */
	public boolean isLocalProfile() {
		if (this.sipDomain != null && this.sipPassword != null)
			return false;
		else
			return true;
	}

	@Override
	public String toString() {
		if (isLocalProfile())
			return userName + "@" + sipDomain + ":" + localSipPort;
		else
			return userName + "@" + sipDomain;
	}

    public boolean isAudio() {
        return audio;
    }

    public boolean isVideo() {
        return video;
    }

    public String getNonce() {
        return nonce;
    }

    public String getRealm() {
        return realm;
    }

    public String getTag() {
        return tag;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }

    public void setSipPassword(String sipPassword) {
        this.sipPassword = sipPassword;
    }

    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }

    public void setAudio(boolean audio) {
        this.audio = audio;
    }

    public void setVideo(boolean video) {
        this.video = video;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
