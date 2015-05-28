package org.zwen.media.protocol.sip;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.parser.URLParser;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.protocol.NoPortAvailableException;
import org.zwen.media.protocol.PortManager;

/**
 * A client read Media(s) from a conference
 * @author chenxiuheng@gmail.com
 */
public class SipDataSource implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SipDataSource.class);

    private int localSipPort;
    private SipUri sipURI;

    private  SipStackExt sipStackExt;
    
    /**
     * @throws NoPortAvailableException 
     * @param sip conference uri, "sip:username:passwd@host:port;parameter=value"
     * @throws ParseException
     * @throws
     */
    public SipDataSource(String conferenceSipUrl) throws ParseException {
        URLParser parser = new URLParser(conferenceSipUrl);
        sipURI = parser.sipURL(true);
        
        
    }

    public void connect() throws IOException {
        try {
            LocalSipProfile localSipProfile = new LocalSipProfile();
            
            String localhost = "10.0.0.1";
            localSipPort = PortManager.findAvailablePorts(1)[0];

            SipStackExt sipStackExt = new SipStackExt();
            sipStackExt.init(localSipProfile, localhost, localSipPort);
        } catch (SipException e) {
            throw new IllegalArgumentException("fail connect to " + sipURI, e);
        } catch (InvalidArgumentException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        } catch (TooManyListenersException e) {
            throw new UnsupportedOperationException("Too Many Listeners", e);
        }
    }

    @Override
    public void close() throws IOException {
        PortManager.removePort(localSipPort);

        if (null != sipStackExt) {
            try {
                sipStackExt.endCall();
                sipStackExt = null;
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public String toString() {
        return sipURI.toString();
    }
}
