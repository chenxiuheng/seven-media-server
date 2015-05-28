package org.zwen.media.protocol.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.TooManyListenersException;
import java.util.Vector;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SipStackExt implements SipListener {
    private static Logger logger = LoggerFactory.getLogger(SipStackExt.class);
    
    private String Tag = "SipService";
    private SipMessageHandler sipMessageHandler;
    private SipManagerState currentState = null;
    private SipSession currentSession = null;
    private SipContact currentContact = null;
    private Request currentRequest = null;
    private SessionDescription currentsdp = null;
    private int registerTryCount = 0;

    public void init(LocalSipProfile localSipProfile,
                               String localIPAddress,
                               int localSipPort) throws PeerUnavailableException,
            TransportNotSupportedException, ObjectInUseException, InvalidArgumentException,
            TooManyListenersException {
        sipMessageHandler =
                SipMessageHandler.newInstance(localSipProfile, localIPAddress, localSipPort);
        sipMessageHandler.getSipProvider().addSipListener(this);
        currentState = SipManagerState.IDLE;
    }

    public void setStatusChanged(SipManagerState state) {
        currentState = state;
        logger.info("{}: {}", state, null);
    }

    public void setStatusChanged(SipManagerState state, String info) {
        currentState = state;
        logger.info("{}: {}", state, info);
    }

    public void setCallStatus(String info) {
        logger.info("{}: {}", SipManagerState.INCOMING, currentSession.getCallerNumber());
    }

    public void setSessionChanged(SipSession sipsession) {
        this.currentSession = sipsession;
    }

    public void register() throws ParseException, InvalidArgumentException, SipException {
        setStatusChanged(SipManagerState.REGISTERING);
        sipMessageHandler.register(SipRequestState.REGISTER);
    }

    public void unregister() throws ParseException, InvalidArgumentException, SipException {
        setStatusChanged(SipManagerState.UNREGISTERING);
        sipMessageHandler.register(SipRequestState.UNREGISTER);
    }

    public void sendInvite(SipContact contact) throws NullPointerException, ParseException,
            InvalidArgumentException, SipException, SdpException {
        currentContact = contact;
        currentSession = null;
        setStatusChanged(SipManagerState.CALLING);
        sipMessageHandler.sendInvite(contact, SipRequestState.REGISTER);
    }

    public void acceptCall() throws SipException, InvalidArgumentException, ParseException,
            SdpException {
        if (currentRequest != null) {
            sipMessageHandler.sendOK(currentRequest, null);
        }
    }

    public void declineCall() throws ParseException, SipException, InvalidArgumentException {
        if (currentRequest != null)
            sipMessageHandler.sendDecline(currentRequest);
    }

    public void endCall() throws ParseException, InvalidArgumentException, SipException {
        if (currentState.equals(SipManagerState.ESTABLISHED))
            sipMessageHandler.sendBye();
        else
            sipMessageHandler.sendCancel();
    }

    public LocalSipProfile getLocalSipProfile() {
        return sipMessageHandler.getLocalSipProfile();
    }

    public void reset() {
        sipMessageHandler.reset();
        currentContact = null;
        currentRequest = null;
        currentSession = null;
        if (currentState.equals(SipManagerState.ESTABLISHED))
            setSessionChanged(null);
        setStatusChanged(SipManagerState.READY);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent arg0) {}

    @Override
    public void processIOException(IOExceptionEvent arg0) {}

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        String method = ((CSeqHeader) request.getHeader(CSeqHeader.NAME)).getMethod();
        System.out.println(Tag + ": Incoming " + method + " request");

        if (method.equals(Request.INVITE)) {
            try {
                if (currentSession == null) {
                    SessionDescription sdpSession =
                            SdpFactory.getInstance().createSessionDescription(new String(request
                                .getRawContent()));
                    currentsdp = sipMessageHandler.createSDP(request);
                    // if there is no codec matched ,so sdp would return null
                    if (currentsdp == null)
                        sipMessageHandler.sendDecline(currentRequest);
                    else {
                        currentSession = SipMessageHandler.createSipSession(request, sdpSession);
                        currentSession.setIncoming(true);
                        // set the audio formats which has negotiated with caller
                        currentSession.setAudioFormats(sipMessageHandler.getSipAudioFormats());
                        String CallTO = currentSession.getToSipURI().toString();
                        CallTO = CallTO.substring(CallTO.indexOf(':') + 1, CallTO.indexOf('@'));
                        if (CallTO.equals(sipMessageHandler.getLocalSipProfile().getUserName())) {
                            currentRequest = request;
                            sipMessageHandler.sendRinging(request);

                            setStatusChanged(SipManagerState.INCOMING, currentSession
                                .getCallerNumber());
                            setCallStatus("Incoming call from " + currentSession.getCallerNumber());
                            System.out.println(Tag + ": " + currentSession.toString());
                        } else {
                            sipMessageHandler.sendNotFound(request);
                        }
                    }
                } else {
                    sipMessageHandler.sendBusyHere(request);
                }
            } catch (Exception e) {
                setStatusChanged(SipManagerState.ERROR);
            }
        } else if (method.equals(Request.CANCEL)) {
            reset();
        } else if (method.equals(Request.ACK)) {
            if (currentSession != null) {
                sipMessageHandler.setDialog(requestEvent.getDialog());

                setStatusChanged(SipManagerState.ESTABLISHED, currentSession.getCallerNumber());
                setSessionChanged(currentSession);
            }
        } else if (method.equals(Request.BYE)) {
            try {
                sipMessageHandler.sendOK(request, requestEvent.getServerTransaction());
            } catch (Exception e) {
                setStatusChanged(SipManagerState.ERROR);
            }
            // no need to send 200 OK, SipStack should do that automatically
            setStatusChanged(SipManagerState.BYE);
            reset();
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = (Response) responseEvent.getResponse();
        int status = response.getStatusCode();
        System.out.println(Tag + ": Response Status Code: " + status);

        switch (status) {
            case 180: // Ringing
                try {
                    if (currentState.equals(SipManagerState.CALLING)) {
                        setStatusChanged(SipManagerState.RINGING);
                    }
                } catch (Exception e) {
                    setStatusChanged(SipManagerState.ERROR);
                }
            case 200: // OK
                try {
                    if ((currentState.equals(SipManagerState.RINGING) || currentState
                        .equals(SipManagerState.CALLING))
                            && response.getRawContent() != null
                            && responseEvent.getDialog() != null) {
                        SessionDescription sdpSession =
                                SdpFactory.getInstance().createSessionDescription(new String(
                                        response.getRawContent()));
                        currentSession = SipMessageHandler.createSipSession(response, sdpSession);
                        Vector audioFormats = sipMessageHandler.getSipAudioFormats(sdpSession);
                        audioFormats = sipMessageHandler.getSipAudioFormatsBump(audioFormats);
                        ArrayList<SipAudioFormat> tempa =
                                sipMessageHandler.getSipAudioFormatsList(audioFormats);
                        currentSession.setAudioFormats(tempa);
                        currentSession.setIncoming(false);
                        sipMessageHandler.setDialog(responseEvent.getDialog());
                        sipMessageHandler.sendAck();

                        setStatusChanged(SipManagerState.ESTABLISHED);
                        setSessionChanged(currentSession);
                    } else if (currentState.equals(SipManagerState.REGISTERING)) {
                        setStatusChanged(SipManagerState.READY);
                        registerTryCount = 0;
                    } else if (currentState.equals(SipManagerState.UNREGISTERING)) {
                        setStatusChanged(SipManagerState.IDLE);
                        registerTryCount = 0;
                    }
                } catch (Exception e) {
                    setStatusChanged(SipManagerState.ERROR);
                }

                break;

            case 401: // Unauthorized
                try {
                    registerTryCount++;

                    WWWAuthenticateHeader authHeader =
                            (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
                    sipMessageHandler.getLocalSipProfile().setNonce(authHeader.getNonce());
                    sipMessageHandler.getLocalSipProfile().setRealm(authHeader.getRealm());

                    if (currentState.equals(SipManagerState.REGISTERING))
                        sipMessageHandler.register(SipRequestState.AUTHORIZATION);
                    else if (currentState.equals(SipManagerState.UNREGISTERING))
                        sipMessageHandler.register(SipRequestState.UNREGISTER_AUTHORIZATION);
                } catch (Exception e) {
                    setStatusChanged(SipManagerState.ERROR);
                }

                break;

            case 404: // Not found
                setStatusChanged(SipManagerState.INVALID);
                break;

            case 407: // Proxy Authentication required
                try {
                    registerTryCount++;
                    System.out.println(Tag + ": " + response.toString());

                    if (currentSession == null && currentContact != null) {
                        ProxyAuthenticateHeader authHeader =
                                (ProxyAuthenticateHeader) response
                                    .getHeader(ProxyAuthenticateHeader.NAME);
                        sipMessageHandler.getLocalSipProfile().setNonce(authHeader.getNonce());
                        sipMessageHandler.getLocalSipProfile().setRealm(authHeader.getRealm());

                        if (currentState.equals(SipManagerState.CALLING))
                            sipMessageHandler.sendInvite(currentContact,
                                                         SipRequestState.AUTHORIZATION);
                    }
                } catch (Exception e) {
                    setStatusChanged(SipManagerState.ERROR);
                }

                break;
            case 480:
                setStatusChanged(SipManagerState.DECLINED);
                break;
            case 486: // Busy
                setStatusChanged(SipManagerState.BUSY);
                break;

            case 603: // Decline
                setStatusChanged(SipManagerState.DECLINED);
                break;

            case 500: // Too many clients
                setStatusChanged(SipManagerState.ERROR);
                break;

            default:
                break;
        }

    }

    @Override
    public void processTimeout(TimeoutEvent arg0) {}

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent arg0) {}

}
