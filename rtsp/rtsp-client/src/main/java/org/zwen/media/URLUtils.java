package org.zwen.media;

import java.util.regex.Pattern;

public class URLUtils {
	public static String concat(String base, String path) {
		if (null == path) {
			return base;
		}
		
		if (Pattern.matches("(([a-z]+)://(.*))", path)) {
			return path;
		} else {
			if (base.endsWith("/")) {
				base = base.substring(0, base.length()-1);
			}
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			
			return base +  path;
		}		
	}
}
