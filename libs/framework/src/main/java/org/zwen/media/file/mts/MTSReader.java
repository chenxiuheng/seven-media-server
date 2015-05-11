package org.zwen.media.file.mts;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer.MTSPacket;
import org.jcodec.containers.mps.MTSUtils.StreamType;
import org.jcodec.containers.mps.psi.PATSection;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;
import org.zwen.media.AVPacket;
import org.zwen.media.Constants;
import org.zwen.media.file.CountableByteChannel;

public class MTSReader implements Closeable {
	private ByteBuffer startCode = ByteBuffer.allocate(1);
	
	// position of MTS start code
	private long position = 0;
	
	final CountableByteChannel ch;

	// pat
	private HashSet<Integer> patPrograms = new HashSet<Integer>();

	// pmt
	private HashMap<Integer, PMTStream> pmtStreams = new HashMap<Integer, PMTStream>();

	// pes streams
	private Map<StreamType, PES> pesStreams = new HashMap<StreamType, PES>();

	public MTSReader(ReadableByteChannel src, Map<StreamType, PESVistor> visitors)  {
		ch = new CountableByteChannel(src);
		
		AtomicLong index = new AtomicLong();
		for (Entry<StreamType, PESVistor> entry : visitors.entrySet()) {
			pesStreams.put(entry.getKey(), new PES(index, entry.getValue()));
		}
	}

	public int readNextMPEGPacket(List<AVPacket> out) throws IOException {
		int pid;
		boolean payloadStart;

		MTSPacket packet = null;
		ByteBuffer buffer = ByteBuffer.allocate(188);

		while (null != (packet = readNextMTS(buffer))) {
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

				pmtStreams.clear();
				for (int i = 0; i < streams.length; i++) {
					pmtStreams.put(streams[i].getPid(), streams[i]);
				}
			}

			// stream
			else if (pmtStreams.containsKey(pid)) {
				PMTStream stream = pmtStreams.get(pid);
				PES pes = pesStreams.get(stream.getStreamType());
				
				ByteBuffer seg = packet.payload;
				if (payloadStart) {
					pes.readPESHeader(position, seg, out);
					return 1;
				} else {
					pes.append(seg);
				}
			}
		}
		
		return -1;
	}

	public void flush(List<AVPacket> out) {
		for (PES pes : pesStreams.values()) {
			pes.flush(out);
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
		File file = new File("test.ts");
		System.out.println(file.exists());

		ReadableByteChannel ch = new FileInputStream(file).getChannel();

		Map<StreamType, PESVistor> vistor = new HashMap<StreamType, PESVistor>();
		vistor.put(StreamType.AUDIO_AAC_ADTS, new DefaultPESVisitor(new AudioFormat(Constants.AAC_ADTS)));
		vistor.put(StreamType.VIDEO_H264, new DefaultPESVisitor(new VideoFormat(Constants.H264_RTP)));
		MTSReader client = new MTSReader(ch, vistor);
		List<AVPacket> out = new ArrayList<AVPacket>();
		while (-1 != client.readNextMPEGPacket(out)) {
			while(!out.isEmpty()) {
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
