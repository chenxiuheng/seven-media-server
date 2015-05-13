package org.zwen.media;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;

public class ByteBuffers {
	public static final ByteBuffer copy(ByteBuffer buf) {
		if (null == buf) {
			return null;
		}

		ByteBuffer newBuf = ByteBuffer.allocate(buf.remaining());
		newBuf.put(buf.duplicate());
		newBuf.flip();

		return newBuf;
	}
	
	public static final String toString(ByteBuffer buf) {
		if(null == buf) {
			return "NULL";
		}
		
		int position = buf.position();
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		buf.position(position);
		
		return new String(Hex.encodeHex(data));
	}
}
