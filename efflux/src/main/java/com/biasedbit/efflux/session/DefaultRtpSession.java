package com.biasedbit.efflux.session;

import com.biasedbit.efflux.participant.ParticipantDatabase;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.SingleParticipantDatabase;

public class DefaultRtpSession extends AbstractRtpSession {

	public DefaultRtpSession(String id, int payloadType, RtpParticipant local) {
		super(id, payloadType, local);
	}


    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected ParticipantDatabase createDatabase() {
        return new SingleParticipantDatabase(this.id);
    }

}
