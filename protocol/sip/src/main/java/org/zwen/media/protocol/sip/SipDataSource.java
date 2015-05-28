package org.zwen.media.protocol.sip;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.parser.URLParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.protocol.PortManager;

/**
 * A client read Media(s) from a conference
 * 
 * @author chenxiuheng@gmail.com
 */
public class SipDataSource {
	private static final Logger logger = LoggerFactory.getLogger(SipDataSource.class);
	
	private String localhost;
	private SipUri sipURI;

	private SipStack sipStack;
	private HeaderFactory headerFactory;
	private AddressFactory addressFactory;
	private MessageFactory messageFactory;

	/**
	 * @param sip
	 *            conference uri,
	 *            "sip:username:passwd@host:port;parameter=value"
	 * @throws ParseException
	 * @throws  
	 */
	public SipDataSource(String sip) throws ParseException {
		URLParser parser = new URLParser(sip);
		sipURI = parser.sipURL(true);
		
		try {
			this.localhost = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new UnsupportedOperationException("Can't get local host address");
		}
	}

	public void connect() throws IOException {
		try {
			SipFactory sipFactory = SipFactory.getInstance();

			sipFactory.setPathName("gov.nist");
			Properties properties = new Properties();
			properties.setProperty("javax.sip.STACK_NAME", "TextClient");
			properties.setProperty("javax.sip.IP_ADDRESS", localhost);

			// DEBUGGING: Information will go to files
			// textclient.log and textclientdebug.log
			properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
			properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
					"textclient.txt");
			properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
					"textclientdebug.log");

			sipStack = sipFactory.createSipStack(properties);
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();

			int[] ports = PortManager.findAvailablePorts(1);
			ListeningPoint udp = sipStack.createListeningPoint("0.0.0.0",
					ports[0], "udp");

			SipProvider sipProvider;
			sipProvider = sipStack.createSipProvider(udp);
			sipProvider.addSipListener(new SipListenerImpl());
			
			
			sipStack.start();
		} catch (SipException e) {
			throw new IllegalArgumentException("fail connect to " + sipURI, e);
		} catch (InvalidArgumentException e) {
			throw new UnsupportedOperationException(e.getMessage(), e);
		} catch (TooManyListenersException e) {
			throw new UnsupportedOperationException("Too Many Listeners", e);
		}
	}

	private final class SipListenerImpl implements SipListener {

		@Override
		public void processDialogTerminated(DialogTerminatedEvent event) {
			// TODO Auto-generated method stub

		}

		@Override
		public void processIOException(IOExceptionEvent event) {
			// TODO Auto-generated method stub

		}

		@Override
		public void processRequest(RequestEvent event) {
			System.out.println(event.getRequest());

		}

		@Override
		public void processResponse(ResponseEvent event) {
			System.out.println(event.getResponse());
		}

		@Override
		public void processTimeout(TimeoutEvent event) {
			// TODO Auto-generated method stub

		}

		@Override
		public void processTransactionTerminated(
				TransactionTerminatedEvent event) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public String toString() {
		return sipURI.toString();
	}
}
