package org.zwen.media.rtp.codec.audio.aac;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwen.media.AVPacket;
import org.zwen.media.AVStreamExtra;
import org.zwen.media.AVTimeUnit;
import org.zwen.media.codec.audio.aac.AACExtra;
import org.zwen.media.rtp.codec.IDePacketizer;

import com.biasedbit.efflux.packet.DataPacket;

public class DePacketizer implements IDePacketizer {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DePacketizer.class);

	@Override
	public AVStreamExtra depacketize(MediaDescription md) throws SdpException {
		Matcher matcher = null;
		AACExtra extra = new AACExtra();

		// a=rtpmap:96 mpeg4-generic/22050/2
		String rtpmap = md.getAttribute(RTPMAP);
		matcher = Pattern.compile("(\\d+) ([^/]+)(/(\\d+)(/(\\d+))?)?(.*)?")
				.matcher(rtpmap);
		if (matcher.matches()) {
			int sampleRate = Integer.valueOf(matcher.group(4));
			int numChannel = 1;
			if (null != matcher.group(6)) {
				numChannel = Integer.valueOf(matcher.group(6));
			}
			extra.setNumChannels(numChannel);
			extra.setSampleRate(sampleRate);
		}

		// a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr;
		// config=139056e5a54800; SizeLength=13; IndexLength=3;
		// IndexDeltaLength=3; Profile=1;
		String fmtp = md.getAttribute(FMTP);
		if (null != fmtp) {

			matcher = Pattern.compile("config=([^;]+)").matcher(fmtp);
			if (matcher.find()) {
				try {
					extra.setConfig(Hex.decodeHex(matcher.group(1)
							.toCharArray()));
				} catch (DecoderException e) {
					LOGGER.warn("fail decode hex [{}]", matcher.group(1));
				}
			}
		}

		return extra;
	}

	@Override
	public void process(DataPacket packet, List<AVPacket> out) {
		
	}

	@Override
	public void setTimeUnit(AVTimeUnit timeUnit) {

	}

}
