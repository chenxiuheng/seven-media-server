package org.zwen.media.protocol.sip;

import java.io.IOException;
import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class TestSipDataSource extends TestCase  {
    private  SipDataSource dataSource ;
    
	@Override
	protected void setUp() throws Exception {
	    Logger logger = LoggerFactory.getLogger(SipStackExt.class);
	    logger.info("a");
	    
	    
		dataSource = new SipDataSource("sip:340000782@114.255.140.107:9060", "1018", "1234");
	}
	
	@Override
	protected void tearDown() throws Exception {
	    dataSource.close();
	}
	
	public void testSipConnect() throws SipException, InvalidArgumentException, TooManyListenersException, IOException, ParseException {
	    dataSource.connect();
	    
	    dataSource.start();
	}
}
