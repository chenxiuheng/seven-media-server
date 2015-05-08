package org.zwen.media.rtp;

import java.util.Comparator;
import java.util.TreeSet;

import javax.media.Buffer;

import net.sf.fmj.media.rtp.RTPSourceStream;

/**
 * Implements an RTP packet queue and the storage-related functionality of a
 * jitter buffer for the purposes of {@link RTPSourceStream}. The effect of a
 * complete jitter buffer is achieved through the combined use of
 * <tt>JitterBuffer</tt> and <tt>JitterBufferBehaviour</tt>.
 *
 */
public class JitterBuffer {
	private int capacity = 4;
	private TreeSet<Buffer> buffer = new TreeSet<Buffer>(seqNumComparator);
	
	public JitterBuffer(){}
	
	public JitterBuffer(int capacity) {
		this.capacity = capacity;
		
		if (capacity < 0 || capacity > (0x8000)) {
			throw new IllegalArgumentException(capacity + " NOT in (0, 2^15)");
		}
	}

	public Buffer add(Buffer pkt) {
		buffer.add(pkt);
		
		if (buffer.size() > capacity) {
			return buffer.pollFirst();
		} else {
			return null;
		}
	}
	
	/**
     * A <tt>Comparator</tt> implementation for RTP sequence numbers.
     * Compares the sequence numbers <tt>a</tt> and <tt>b</tt>
     * of <tt>pkt1</tt> and <tt>pkt2</tt>, taking into account the wrap at 2^16.
     *
     * IMPORTANT: This is a valid <tt>Comparator</tt> implementation only if
     * used for subsets of [0, 2^16) which don't span more than 2^15 elements.
     *
     * E.g. it works for: [0, 2^15-1] and ([50000, 2^16) u [0, 10000])
     * Doesn't work for: [0, 2^15] and ([0, 2^15-1] u {2^16-1}) and [0, 2^16)
     */
    public static final Comparator<? super Buffer> seqNumComparator
            = new Comparator<Buffer>() {
        @Override
        public int compare(Buffer pkt1, Buffer pkt2)
        {
            long a = pkt1.getSequenceNumber();
            long b = pkt2.getSequenceNumber();

            if (a == b)
                return 0;
            else if (a > b)
            {
                if (a - b < 32768)
                    return 1;
                else
                    return -1;
            }
            else //a < b
            {
                if (b - a < 32768)
                    return -1;
                else
                    return 1;
            }
        }
    };
}
