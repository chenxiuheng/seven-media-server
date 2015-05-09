package org.zwen.media;


public class AVTimeUnit {
	public static final AVTimeUnit MILLISECONDS = new AVTimeUnit(1, 1);
	
	/** 1/90 毫秒 */
	public static final AVTimeUnit MILLISECONDS_90 = new AVTimeUnit(1, 90)
	;
	private int num;
	private int base; 

	private AVTimeUnit(int num, int base) {
		this.num = num;
		this.base = base;
	}

	public long convert(long sourceDuration, AVTimeUnit sourceUnit) {
		if (sourceUnit == this) {
			return sourceDuration;
		}
		
		long dstDuration = 0;

		dstDuration = (sourceDuration * sourceUnit.num * base / num / sourceUnit.base );

		return dstDuration;
	}

	@Override
	public String toString() {
		return "TimeUnit" + num + "/" + base + "s";
	}
}
