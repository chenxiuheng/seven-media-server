package org.zwen.media.protocol.sip;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import junit.framework.TestCase;

import org.zwen.media.protocol.PortManager;

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
		

		String to = "sip:340000782@114.255.140.107:9060";
		String message = "say hello";
		
	}
}
