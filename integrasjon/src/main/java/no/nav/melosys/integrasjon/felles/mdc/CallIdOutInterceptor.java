package no.nav.melosys.integrasjon.felles.mdc;

import java.util.List;
import java.util.Objects;
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
        //Call Id egentlig settes i MDCFilter og Forespørsel til gui tjeneste kaller MDCFilter. Gjennom initialisering av Melosys App, KodeVerkService henter kodeverk med bruk av Kodeverk Web Service og legger inn i cache. I det tilfelle MDCFilter blir ikke kallet fordi at det er ikke noen forespørsel fra gui tjeneste lag. Så vi må sette Call Id eksplisitt.
        //TODO: vi trenger bedre løsning enn bare sette Call Id fordi det kan påvirker alle tjenester siden alle bruker denne interceptor men det er veldig liten sannsynlighet at MDFilter klarer ikke å sette Call Id.
        if (Objects.isNull(MDCOperations.getFromMDC(MDC_CALL_ID))) {
            putToMDC(MDC_CALL_ID, generateCallId());
        }
        String callId = MDCOperations.getFromMDC(MDC_CALL_ID);

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
