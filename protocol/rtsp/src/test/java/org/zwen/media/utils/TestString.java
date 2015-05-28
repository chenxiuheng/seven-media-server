package org.zwen.media.utils;

import junit.framework.TestCase;

public class TestString extends TestCase {
	public void testStringFormat() throws Exception {
		System.out.println(String.format("a%s,%s, %d", "tt", "gg", 1));
	}
}
