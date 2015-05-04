package org.zwen.media.server.rtp;

import javax.media.Buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biasedbit.efflux.packet.DataPacket;

public class JitterBuffer {
	private static final Logger logger = LoggerFactory.getLogger(JitterBuffer.class);

	/**
	 * The capacity of this instance in terms of the maximum number of
	 * <tt>Buffer</tt>s that it may contain.
	 */
	private int capacity;

	/**
	 * The <tt>Buffer</tt>s of this <tt>JitterBuffer</tt> which may contain
	 * valid media data to be read out of this instance (referred to as
	 * &quot;fill&quot;) or may represent preallocated <tt>Buffer</tt> instances
	 * for the purposes of reducing the effects of allocation and garbage
	 * collection (referred to as &quot;free&quot;). The storage is of a
	 * circular nature with the first &quot;fill&quot; at index {@link #offset}
	 * and the number of &quot;fill&quot; equal to {@link #length}.
	 */
	private DataPacket[] elements;

	/**
	 * The number of &quot;fill&quot; <tt>Buffer</tt>s in {@link #elements}.
	 */
	private int length;

	/**
	 * The index in {@link #elements} of the <tt>Buffer</tt>, if any, which has
	 * been retrieved from this queue and has not been returned yet.
	 */
	private int locked;

	/**
	 * The index in {@link #elements} of the first &quot;fill&quot;
	 * <tt>Buffer</tt>.
	 */
	private int offset;

	public JitterBuffer(int capacity) {
		this.elements = new DataPacket[capacity];
		this.capacity = capacity;
		 length = 0;
        locked = -1;
        offset = 0;
	}

	public void addPkt(DataPacket pkt) {
		assertLocked(pkt);
		if (noMoreFree())
			throw new IllegalStateException("noMoreFree");

		long firstSeq = getFirstSeq();
		long lastSeq = getLastSeq();
		long bufferSN = pkt.getSequenceNumber();

		if (firstSeq == Buffer.SEQUENCE_UNKNOWN
				&& lastSeq == Buffer.SEQUENCE_UNKNOWN) {
			// it's the first packet
			append(pkt);
			return;
		}
		
		// seqNo recycled
		if (firstSeq > lastSeq) {
			lastSeq += 65535;
		}
		
		if (lastSeq < bufferSN) {
			append(pkt);
		} else if (firstSeq < bufferSN && bufferSN < lastSeq) {
			insert(pkt);
		} else if (bufferSN < firstSeq) {
			if (bufferSN >= firstSeq - getFreeCount()) {
				prepend(pkt);
			} else if (lastSeq + getFreeCount() > 65535 && bufferSN <= (lastSeq + getFreeCount()) % 65535) {
				pkt.setSequenceNumber(bufferSN);
				append(pkt);
			} else {
				returnFree(pkt);
				return;
			}
		} else { //only if (bufferSN == firstSN) || (bufferSN == lastSN)?
			returnFree(pkt);
			return;
		}

		logger.debug("addPkt {}", pkt);
	}

	/**
	 * Determines whether there are no more &quot;free&quot; elements/
	 * <tt>Buffer</tt>s in this queue.
	 * 
	 * @return <tt>true</tt> if there are no more &quot;free&quot; elements/
	 *         <tt>Buffer</tt>s in this queue; otherwise, <tt>false</tt>
	 */
	boolean noMoreFree() {
		return (getFreeCount() == 0);
	}

	/**
	 * Gets the number of &quot;free&quot; <tt>Buffer</tt>s in this queue.
	 * 
	 * @return the number of &quot;free&quot; <tt>Buffer</tt>s in this queue
	 */
	public int getFreeCount() {
		return (capacity - length);
	}

	/**
	 * Gets the sequence number of the element/<tt>Buffer</tt> at the head of
	 * this queue or <tt>Buffer.SEQUENCE_UNKNOWN</tt> if this queue is empty.
	 * 
	 * @return the sequence number of the element/<tt>Buffer</tt> at the head of
	 *         this queue or <tt>Buffer.SEQUENCE_UNKNOWN</tt> if this queue is
	 *         empty.
	 */
	public long getFirstSeq() {
		return (length == 0) ? Buffer.SEQUENCE_UNKNOWN : elements[offset]
				.getSequenceNumber();
	}

	/**
	 * Gets the sequence number of the element/<tt>Buffer</tt> at the tail of
	 * this queue or <tt>Buffer.SEQUENCE_UNKNOWN</tt> if this queue is empty.
	 * 
	 * @return the sequence number of the element/<tt>Buffer</tt> at the tail of
	 *         this queue or <tt>Buffer.SEQUENCE_UNKNOWN</tt> if this queue is
	 *         empty.
	 */
	public long getLastSeq() {
		return (length == 0) ? Buffer.SEQUENCE_UNKNOWN : elements[(offset
				+ length - 1)
				% capacity].getSequenceNumber();
	}

	/**
	 * Adds <tt>buffer</tt> to the beginning of this queue.
	 * 
	 * @param buffer
	 *            the <tt>Buffer</tt> to add to the beginning of this queue
	 */
	private void prepend(DataPacket buffer) {
		int index = offset - 1;

		if (index < 0)
			index = capacity - 1;
		if (index != locked) {
			elements[locked] = elements[index];
			elements[index] = buffer;
		}
		offset = index;
		length++;
	}

	/**
	 * Adds <tt>buffer</tt> to the end of this queue.
	 * 
	 * @param buffer
	 *            the <tt>Buffer</tt> to be added to the end of this queue
	 */
	private void append(DataPacket buffer) {
		int index = (offset + length) % capacity;

		if (index != locked) {
			elements[locked] = elements[index];
			elements[index] = buffer;
		}
		length++;
	}

	/**
	 * Inserts <tt>buffer</tt> in the correct place in the queue, so that the
	 * order is preserved. The order is by ascending sequence numbers.
	 * 
	 * Note: This could potentially be slow, since all the elements 'bigger'
	 * than <tt>buffer</tt> are moved.
	 * 
	 * @param buffer
	 *            the <tt>Buffer</tt> to insert
	 */
	private void insert(DataPacket buffer) {
		int end = (offset + length) % capacity;
		int i = end - 1;
		long bufferSN = buffer.getSequenceNumber();

		
		while (i != offset) {
			if (elements[i].getSequenceNumber() > bufferSN)
				break;
			if (--i < 0)
				i = capacity - 1;
		}

		if (i == offset)
			prepend(buffer);
		else if (i == end)
			append(buffer);
		else {
			elements[locked] = elements[end];
			for (int j = end; j != i;) {
				int k = j - 1;

				if (k < 0)
					k = capacity - 1;
				elements[j] = elements[k];
				j = k;
			}
			elements[i] = buffer;
			length++;
		}
	}

	/**
	 * Returns (releases) <tt>buffer</tt> to the <tt>free</tt> queue.
	 * 
	 * @param buffer
	 *            the <tt>Buffer</tt> to return
	 */
	public void returnFree(DataPacket buffer) {
		assertLocked(buffer);

		locked = -1;
	}

	/**
	 * Asserts that a <tt>Buffer</tt> has been retrieved from this
	 * <tt>JitterBuffer</tt> and has not been returned yet.
	 * 
	 * @throws IllegalStateException
	 *             if no <tt>Buffer</tt> has been retrieved from this
	 *             <tt>JitterBuffer</tt> and has not been returned yet
	 */
	private void assertLocked(DataPacket buffer) throws IllegalStateException {
		if (locked == -1) {
			throw new IllegalStateException(
					"No Buffer has been retrieved from this JitterBuffer"
							+ " and has not been returned yet.");
		}
		if (buffer != elements[locked])
			throw new IllegalArgumentException("buffer");
	}

}
