package org.zwen.media.protocol.sip;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelephoneNumber;
import gov.nist.javax.sip.parser.URLParser;

import java.io.IOException;
import java.net.InetAddress;
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

import org.zwen.media.protocol.PortManager;

import junit.framework.TestCase;

public class TestSipClient extends TestCase  {
    private  SipDataSource dataSource ;
    
	@Override
	protected void setUp() throws Exception {
		dataSource = new SipDataSource("sip:340000782@114.255.140.107:9060");
	}
	
	public void testSipConnect() throws SipException, InvalidArgumentException, TooManyListenersException, IOException, ParseException {
		String username = "1019";
		String ip = InetAddress.getLocalHost().getHostAddress();
		int port = PortManager.findAvailablePorts(1, 5060)[0];
		
		SipLayer layer = new SipLayer(username, ip, port);

		String to = "sip:340000782@114.255.140.107:9060";
		String message = "say hello";
		layer.sendMessage(to, message);
	}
}
