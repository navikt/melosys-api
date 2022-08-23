package no.nav.melosys.integrasjon.felles.mdc

import org.apache.cxf.binding.soap.SoapHeader
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.interceptor.Fault
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.message.Message
import org.apache.cxf.phase.AbstractPhaseInterceptor
import org.apache.cxf.phase.Phase
import javax.xml.bind.JAXBException

class CorrelationIdOutgoingSOAPInterceptor() : AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM) {

    override fun handleMessage(message: Message?) {
        /* Call Id settes normalt i MDCFilter etter en forespørsel fra frontend-api.
         * Etter initialisering av Melosys, KodeverkService henter kodeverk og legger det inn i en cache.
         * I det tilfellet er MDCFilter ikke kalt fordi det ikke er noen forespørsel fra frontend-api. Derfor må vi sette Call Id eksplisitt.
         */
        var callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID)
        if (callId == null) {
            callId = MDCOperations.generateCallId()
            MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, callId)
        }
        val soapMessage: SoapMessage = if (message is SoapMessage) {
            message
        } else {
            throw IllegalStateException("message har uventet type")
        }
        val list = soapMessage.headers
        try {
            val header = SoapHeader(
                MDCOperations.CALLID_QNAME, callId, JAXBDataBinding(
                    String::class.java
                )
            )
            list.add(header)
        } catch (e: JAXBException) {
            throw Fault(e)
        }
    }
}
