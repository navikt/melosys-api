package no.nav.melosys.integrasjon.felles.mdc

import org.slf4j.MDC
import java.security.SecureRandom
import javax.xml.namespace.QName

class MDCOperations {

    companion object {
        @JvmField
        val MDC_CALL_ID = "callId"
        @JvmField
        val MDC_USER_ID = "userId"
        @JvmField
        val MDC_CONSUMER_ID = "consumerId"
        @JvmField
        val CORRELATION_ID = "correlation-id"
        @JvmField
        val X_CORRELATION_ID = "X-Correlation-ID";

        // QName for the callId header
        @JvmField
        val CALLID_QNAME = QName("uri:no.nav.applikasjonsrammeverk", MDC_CALL_ID)

        @JvmStatic
        fun getFromMDC(key: String?): String? {
            return MDC.get(key)
        }

        @JvmStatic
        fun putToMDC(key: String?, value: String?) {
            MDC.put(key, value)
        }

        @JvmStatic
        fun remove(key: String?) {
            MDC.remove(key)
        }

        @JvmStatic
        fun generateCallId(): String {
            val randomNr = SecureRandom().nextInt(Int.MAX_VALUE)
            val systemTime = System.currentTimeMillis()
            val callId = StringBuilder()
            callId.append("CallId_")
            callId.append(systemTime)
            callId.append('_')
            callId.append(randomNr)
            return callId.toString()
        }
    }

}
