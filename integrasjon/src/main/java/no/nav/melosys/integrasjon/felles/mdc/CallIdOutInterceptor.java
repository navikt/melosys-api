package no.nav.melosys.integrasjon.felles.mdc;

import java.util.List;
import javax.xml.bind.JAXBException;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.MDC_CALL_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.getFromMDC;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.putToMDC;

/**
 * Interceptor som brukes til å sette MDC (Mapped Diagnostic Context) med bruker og kall-id.
 */
public class CallIdOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public CallIdOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) {
        /* Call Id settes normalt i MDCFilter etter en forespørsel fra frontend-api.
         * Etter initialisering av Melosys, KodeverkService henter kodeverk og legger det inn i en cache.
         * I det tilfellet er MDCFilter ikke kalt fordi det ikke er noen forespørsel fra frontend-api. Derfor må vi sette Call Id eksplisitt.
         */
        String callId = getFromMDC(MDC_CALL_ID);
        if (callId == null) {
            callId = generateCallId();
            putToMDC(MDC_CALL_ID, callId);
        }

        SoapMessage soapMessage;
        if (message instanceof SoapMessage) {
            soapMessage = (SoapMessage) message;
        } else {
            throw new IllegalStateException("message har uventet type");
        }
        List<Header> list = soapMessage.getHeaders();
        try {
            SoapHeader header = new SoapHeader(MDCOperations.CALLID_QNAME, callId, new JAXBDataBinding(String.class));
            list.add(header);
        } catch (JAXBException e) {
            throw new Fault(e);
        }
    }
}
