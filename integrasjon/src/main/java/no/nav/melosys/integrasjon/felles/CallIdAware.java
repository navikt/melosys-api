package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;

public interface CallIdAware {
    default String getCallID() {
        String callID = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID);
        if (callID == null) {
            callID = MDCOperations.generateCallId();
        }
        return callID;
    }
}
