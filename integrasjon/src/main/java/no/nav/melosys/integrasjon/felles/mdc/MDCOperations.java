package no.nav.melosys.integrasjon.felles.mdc;

import java.security.SecureRandom;

import javax.xml.namespace.QName;

import org.slf4j.MDC;

/**
 * Utility-klasse for kommunikasjon med MDC. (Opprinnelig fra modig-log-common)
 */
public final class MDCOperations {

    public static final String MDC_CALL_ID = "callId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CONSUMER_ID = "consumerId";

    // QName for the callId header
    public static final QName CALLID_QNAME = new QName("uri:no.nav.applikasjonsrammeverk", MDC_CALL_ID);

    private static final SecureRandom RANDOM = new SecureRandom();

    private MDCOperations() {
    }

    public static String generateCallId() {
        int randomNr = getRandomNumber();
        long systemTime = getSystemTime();

        StringBuilder callId = new StringBuilder();
        callId.append("CallId_");
        callId.append(systemTime);
        callId.append('_');
        callId.append(randomNr);

        return callId.toString();
    }

    public static String getFromMDC(String key) {
        return MDC.get(key);
    }

    public static void putToMDC(String key, String value) {
        MDC.put(key, value);
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    private static int getRandomNumber() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    private static long getSystemTime() {
        return System.currentTimeMillis();
    }
}