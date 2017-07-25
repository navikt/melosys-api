package no.nav.melosys.integrasjon.felles;

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

/**
 * Interceptor som brukes til å sette MDC (Mapped Diagnostic Context) med bruker og kall-id.
 */
public class CallIdOutInterceptor extends AbstractPhaseInterceptor<Message> {

    //TODO Francois
    private boolean ready = false;

    public CallIdOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) {

        if(!ready) {
            return;
        }

        String callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID);
        if (callId == null) {
            throw new IllegalStateException("CallId skal være tilgjengelig i MDC på dette tidspunkt.");
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
