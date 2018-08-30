package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public interface RestConsumer {

    default String getCallID() {
        String callID = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID);
        if (callID == null) {
            callID = MDCOperations.generateCallId();
        }
      return callID;
    }

    default String getUserID() {
        return SubjectHandler.getInstance().getUserID();
    }

    default String getBearer() {
        return "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
    }
}
