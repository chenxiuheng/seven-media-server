package org.zwen.media.file.mts;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer.MTSPacket;
import org.jcodec.containers.mps.MTSUtils.StreamType;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVDispatcher;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStream;
import org.zwen.media.Constants;
import org.zwen.media.file.CountableByteChannel;
import org.zwen.media.file.mts.vistor.AACVistor;
import org.zwen.media.file.mts.vistor.H264Visitor;

public class MTSReader implements Closeable {
	private static org.slf4j.Logger LOGGER = LoggerFactory
			.getLogger(MTSReader.class);

	private static final Comparator<? super AVPacket> COMPARATOR = new Comparator<AVPacket>() {
		@Override
		public int compare(AVPacket pkt1, AVPacket pkt2) {
			long a = pkt1.getSequenceNumber();
			long b = pkt2.getSequenceNumber();

			long p1 = pkt1.getPts();
			long p2 = pkt2.getPts();
			if (p1 != p2) {
				return p1 < p2 ? -1 : 1;
			}
			
			if (a == b) {
				return 0;
			} else if (a > b) {
				return 1;
			} else // a < b
			{
				return -1;
			}
		}
	};

	// position of MTS start code
	private ByteBuffer startCode = ByteBuffer.allocate(1);
	private long position = 0;
	private boolean closed;

	final CountableByteChannel ch;
	final AtomicLong sysClock = new AtomicLong(300);

	// pat
	private HashSet<Integer> patPrograms = new HashSet<Integer>();

	// pmt
	private int pmt;

	// pes streams
	private Map<StreamType, PESVistor> visitors = Collections.emptyMap();
	private PES[] pesStreams;

	private boolean dispatchedAVStreams = false;
	private int bufferLength = 50; // 0.5s for video which is 25fps
	private TreeSet<AVPacket> buffers = new TreeSet<AVPacket>(COMPARATOR);

	public MTSReader(ReadableByteChannel src,
			Map<StreamType, PESVistor> visitors) {
		ch = new CountableByteChannel(src);

		this.visitors = visitors;
	}

	public AVStream[] getAvstream() {
		return null;
	}

	public int read(AVDispatcher dispatcher) throws IOException {
		int pid;
		boolean payloadStart;
		boolean foundMPEGPacket = false;

		MTSPacket packet = null;
		ByteBuffer buffer = ByteBuffer.allocate(188);

		while (!foundMPEGPacket && null != (packet = readNextMTS(buffer))) {
			pid = packet.pid;
			payloadStart = packet.payloadStart;

			// pat
			if (0 == pid) {
				packet.payload.get(); // pointer
				PATSection pat = PATSection.parse(packet.payload);
				patPrograms.clear();

				int[] pmt = pat.getPrograms().values();
				for (int i = 0; i < pmt.length; i++) {
					patPrograms.add(pmt[i]);
				}
			}
			// pmt
			else if (patPrograms.contains(pid)) {
				packet.payload.get(); // pointer
				PMTSection pmt = PMTSection.parse(packet.payload);
				PMTStream[] streams = pmt.getStreams();

				if (this.pmt != pid) {
					pesStreams = new PES[streams.length];
					for (int i = 0; i < streams.length; i++) {
						pid = streams[i].getPid();
						StreamType streamType = streams[i].getStreamType();
						AVStream av = new AVStream(sysClock, i);

						switch (streamType) {
						case AUDIO_AAC_ADTS:
							av.setFormat(new AudioFormat(Constants.AAC));
							break;
						case VIDEO_H264:
							av.setFormat(new VideoFormat(Constants.H264));
							break;
						default:
							if (streamType.isVideo()) {
								av
										.setFormat(new VideoFormat(streamType
												.name()));
							} else if (streamType.isAudio()) {
								av
										.setFormat(new AudioFormat(streamType
												.name()));
							}
							break;
						}
						;

						pesStreams[i] = new PES(pid, visitors.get(streamType),
								av);
					}

				}
			}

			// stream
			else if (null != pesStreams) {
				boolean foundIt = false;
				for (int i = 0; i < pesStreams.length; i++) {
					PES pes = pesStreams[i];
					if (pes.pid == pid) {
						foundIt = true;
						ByteBuffer seg = packet.payload;
						if (payloadStart) {
							pes.readPESHeader(position, seg, buffers);
						} else {
							pes.append(seg);
						}

						int numPkts = dispatch(dispatcher, bufferLength);
						if (numPkts > 0) {
							foundMPEGPacket = true;
						}
					}
				}

				if (!foundIt) {
					LOGGER.warn("NOT FOUND PES[{}]", pid);
				}
			}
		}

		if (foundMPEGPacket) {
			return 1;
		} else if (!foundMPEGPacket && !closed) {
			closed = true;
			return flush(dispatcher);
		} else {
			return -1;
		}
	}

	public int flush(AVDispatcher dispatcher) {
		for (PES pes : pesStreams) {
			pes.flush(buffers);
		}

		return dispatch(dispatcher, 0);
	}

	private int dispatch(AVDispatcher dispatcher, int minBuffer) {
		int numPkt = 0;
		while (buffers.size() > minBuffer) {
			dispatchAVStreamsIfNeed(dispatcher);

			AVPacket pkt = buffers.pollFirst();
			AVStream stream = pesStreams[pkt.getStreamIndex()].stream;

			if (!stream.getFormat().equals(pkt.getFormat())) {
				LOGGER.error("AVStream[{}] with AVPacket NOT SAME Format",
						stream.getFormat(), pkt.getFormat());
			}

			dispatcher.firePacket(stream, pkt);
			numPkt++;
		}

		return numPkt;
	}

	private void dispatchAVStreamsIfNeed(AVDispatcher dispatcher) {
		if (!dispatchedAVStreams) {
			dispatchedAVStreams = true;
			AVStream[] avs = new AVStream[pesStreams.length];
			for (int i = 0; i < avs.length; i++) {
				avs[i] = pesStreams[i].stream;
			}
			dispatcher.fireSetup(avs);
		}
	}

	private MTSPacket readNextMTS(ByteBuffer buffer) throws IOException {
		for (;;) {
			// find start code
			startCode.clear();
			while (ch.read(startCode) > 0) {
				if (startCode.get(0) == 0x47) {
					position = ch.position() - 1;
					break;
				}
			}
			startCode.flip();

			// read 187
			buffer.clear();
			buffer.put(startCode);
			while (buffer.hasRemaining() && ch.read(buffer) > 0) {

			}

			// decode
			if (!buffer.hasRemaining()) {
				buffer.flip();
				MTSPacket packet = MTSDemuxer.parsePacket(buffer);
				return packet;
			} else {
				return null; // EOF
			}
		}
	}

	public static void main(String[] args) throws IOException {
		File file = new File("media_w206756986_0.ts");
		System.out.println(file.exists());

		ReadableByteChannel ch = new FileInputStream(file).getChannel();

		Map<StreamType, PESVistor> vistor = new HashMap<StreamType, PESVistor>();
		vistor.put(StreamType.AUDIO_AAC_ADTS, new AACVistor());
		vistor.put(StreamType.VIDEO_H264, new H264Visitor());
		MTSReader client = new MTSReader(ch, vistor);
		List<AVPacket> out = new ArrayList<AVPacket>();

		AVDispatcher dispatcher = new AVDispatcher();
		while (-1 != client.read(dispatcher)) {
			while (!out.isEmpty()) {
				AVPacket av = out.remove(0);
				System.out.println(av);
			}
		}
	}

	@Override
	public void close() throws IOException {
		ch.close();
	}

}
