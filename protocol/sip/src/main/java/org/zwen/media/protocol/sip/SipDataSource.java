package org.zwen.media.protocol.sip;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.parser.URLParser;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.TooManyListenersException;

import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.protocol.PortManager;

/**
 * A client read Media(s) from a conference
 * @author chenxiuheng@gmail.com
 */
public class SipDataSource implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SipDataSource.class);

    private String localSipHost;
    private int localSipPort;
    private int localAudioPort;
    private int localVideoPort;

    private String user;
    private String passwd;
    private SipUri destURI;

    private SipStackExt sipStackExt;
    private SipContact sipContact;
    private LocalSipProfile localSipProfile;


    /**
     * use the user to join the conference
     * @param conferenceSipUrl "sip:username:passwd@host:port;parameter=value"
     * @param user
     * @param passwd
     * @throws ParseException
     */
    public SipDataSource(String conferenceSipUrl, String user, String passwd) throws ParseException {
        URLParser parser = new URLParser(conferenceSipUrl);
        this.destURI = parser.sipURL(true);
        this.user = user;
        this.passwd = passwd;
    }

    public void connect() throws IOException {
        try {
            localSipHost = InetAddress.getLocalHost().getHostAddress();
            localSipPort = PortManager.findAvailablePorts(1)[0];
            localAudioPort = PortManager.findAvailablePorts(1, localSipPort + 2)[0];
            localVideoPort = PortManager.findAvailablePorts(1, localSipPort + 4)[0];

            localSipProfile = new LocalSipProfile();
            localSipProfile.setAudio(true);
            localSipProfile.setVideo(true);
            localSipProfile.setDisplayName(user);
            localSipProfile.setLocalAudioRtpPort(localAudioPort);
            localSipProfile.setLocalVideoRtpPort(localVideoPort);
            localSipProfile.setLocalSipPort(localSipPort);
            localSipProfile.setSipDomain(destURI.getHost());
            localSipProfile.setRealm(destURI.getHost());
            localSipProfile.setSipPassword(passwd);
            localSipProfile.setSipPort(destURI.getPort());
            localSipProfile.setUserName(user);

            // create a list of supported audio formats for the local user agent
            ArrayList<SipAudioFormat> audioFormats = new ArrayList<SipAudioFormat>();
            audioFormats.add(new SipAudioFormat(SdpConstants.PCMU, "PCMU", 8000));
            audioFormats.add(new SipAudioFormat(SdpConstants.PCMA, "PCMA", 8000));
            audioFormats.add(new SipAudioFormat(SdpConstants.G722, "G722", 8000));
            localSipProfile.setAudioFormats(audioFormats);

            // create a list of supported video formats for the local user agent
            ArrayList<SipVideoFormat> videoFormats = new ArrayList<SipVideoFormat>();
            videoFormats.add(new SipVideoFormat(96, "VP8", 90000));
            videoFormats.add(new SipVideoFormat(98, "H264", 90000));
            localSipProfile.setVideoFormats(videoFormats);



            if (!localSipProfile.isLocalProfile()) {
                logger.warn("Not Local {}", localSipProfile);
            }

            sipStackExt = new SipStackExt();
            sipStackExt.init(localSipProfile, localSipHost, localSipPort);
            sipStackExt.register();

            sipContact = new SipContact(user, localSipHost, localSipPort);
            sipStackExt.sendInvite(sipContact);
        } catch (SipException e) {
            throw new IllegalArgumentException("fail connect to " + destURI, e);
        } catch (InvalidArgumentException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        } catch (TooManyListenersException e) {
            throw new UnsupportedOperationException("Too Many Listeners", e);
        } catch (ParseException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SdpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void start() {
       
    }

    @Override
    public void close() throws IOException {
        PortManager.removePort(localSipPort);
        PortManager.removePort(localAudioPort);
        PortManager.removePort(localVideoPort);

        if (null != sipStackExt) {
            try {
                sipStackExt.endCall();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            } finally {
                try {
                    sipStackExt.unregister();
                } catch (Exception e) {
                    logger.debug("fail unregister {}", e.getMessage(), e);
                }
                sipStackExt = null;
            }
        }
    }

    @Override
    public String toString() {
        return destURI.toString();
    }
}
