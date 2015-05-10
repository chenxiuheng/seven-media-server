package org.zwen.media.protocol.rtsp.sdp.video.h264;

import gov.nist.core.Separators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FmtpValues implements SDPFieldValues {
	final static private Logger logger = LoggerFactory
			.getLogger(FmtpValues.class);
	
	private int type;
	private List<Entry> configs;

	public FmtpValues(int type, List<Entry> configs) {
		this.type = type;
		this.configs = configs;
	}

	/**
	 * 解析 rtsp 中的内容
	 * 
	 * @param config
	 *            fmtp:96 packetization-mode=1;profile-level-id=4D001F;sprop-parameter-sets=Z00AH9oBQBbpUgAAAwACAAADAGTAgAC7fgAD9H973wvCIRqA,aM48gA==
	 * @return
	 */
	public static FmtpValues parse(String fmpt) {
		if (null == fmpt) {
			return null;
		}

		Pattern pattern = Pattern.compile("(fmtp:)?([0-9]+)\\s(.*)?");
		Matcher matcher = pattern.matcher(fmpt);
		if(!matcher.find()) {
			throw new IllegalArgumentException(fmpt + " NOT start with fmpt");
		}
		
		int fmptType = Integer.valueOf(matcher.group(2));
		String config = matcher.group(3);
		String[] props = StringUtils.split(config, ';');
		ArrayList<Entry> configs = new ArrayList<Entry>(props.length);
		for (int i = 0; i < props.length; i++) {
			String prop = props[i];
			int indexOfSpliter = prop.indexOf('=');

			Entry entry;
			if (indexOfSpliter < 0) {
				entry = new Entry(prop, null);
			} else {
				entry = new Entry(prop.substring(0, indexOfSpliter), prop
						.substring(indexOfSpliter + 1));
			}
			configs.add(entry);
		}

		return new FmtpValues(fmptType, configs);
	}

	public String getValue(String key) {
		for (Entry entry : configs) {
			if (entry.key.equals(key)) {
				return entry.getValue();
			}
		}

		return null;
	}

	public byte[] getBase64Value(String key) {
		for (Entry entry : configs) {
			if (entry.key.equals(key)) {
				return entry.getBase64Value();
			}
		}

		return null;
	}

	public static class Entry implements Map.Entry<String, String>,
			Serializable {
		private static final long serialVersionUID = 1L;
		private String key;
		private String value;

		public Entry(String key) {
			this.key = key;
		}

		public Entry(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getValue() {
			return value;
		}

		public byte[] getBase64Value() {
			if (null == value) {
				return null;
			}

			byte[] dst = Base64.decodeBase64(value.getBytes());
			return dst;
		}

		@Override
		public String setValue(String value) {
			String valueReturn = this.value;
			this.value = value;

			return valueReturn;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(FMTP).append(Separators.COLON).append(type).append(" ");
		
		boolean isFirst = true;
		for (FmtpValues.Entry entry : configs) {
			if (!isFirst) {
				buf.append(";");
			}
			isFirst = false;
			
			buf.append(entry.getKey()).append("=").append(null != entry.getValue() ? entry.getValue() : "");
		}
		return buf.toString();
	}
}
