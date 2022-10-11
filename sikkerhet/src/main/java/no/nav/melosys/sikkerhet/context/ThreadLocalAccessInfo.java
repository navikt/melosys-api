package no.nav.melosys.sikkerhet.context;

import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalAccessInfo {
    private static final Logger log = LoggerFactory.getLogger(ThreadLocalAccessInfo.class);
    private String requestUri;
    private UUID processId;
    private String prosessSteg;
    private boolean isAdminRequest;

    private String saksbehandler;

    private boolean isFromWebRequest() {
        return requestUri != null;
    }

    private boolean isFromProcess() {
        return processId != null;
    }

    private boolean isFromAdminRequest() {
        return isAdminRequest;
    }

    private String getUserID() {
        return saksbehandler;
    }

    public static String getSaksbehandler() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.getUserID();
    }

    private static final ThreadLocal<ThreadLocalAccessInfo> threadLocalStorage =
        ThreadLocal.withInitial(ThreadLocalAccessInfo::new);

    public static void beforeControllerRequest(String requestUri, boolean isAdminRequest) {
        ThreadLocalAccessInfo threadLocalAccessInfo = threadLocalStorage.get();
        if (threadLocalAccessInfo.requestUri != null) {
            throw new IllegalStateException("We should not have a thread local requestUri before controller request");
        }
        threadLocalAccessInfo.requestUri = requestUri;
        threadLocalAccessInfo.isAdminRequest = isAdminRequest;
    }

    public static void afterControllerRequest(String requestUri) {
        if (!threadLocalStorage.get().requestUri.equals(requestUri)) {
            throw new IllegalStateException("start and end request should be equal \n"
                + threadLocalStorage.get().requestUri + " != " + requestUri);
        }
        threadLocalStorage.remove();
    }

    public static void executeProcess(String prosessSteg, Runnable runnable) {
        UUID processId = UUID.randomUUID();
        executeProcess(processId, prosessSteg, runnable);
    }

    public static void executeProcess(UUID processId, String prosessSteg, Runnable runnable) {
        beforeExecuteProcess(processId, prosessSteg);
        try {
            runnable.run();
        } finally {
            afterExecuteProcess(processId);
        }
    }

    public static void beforeExecuteProcess(UUID processId, String prosessSteg) {
        beforeExecuteProcess(processId, prosessSteg, null);
    }

    public static void beforeExecuteProcess(UUID processId, String prosessSteg, String saksbehandler) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.processId != null || threadLocalAccessInfo.prosessSteg != null) {
            throw new IllegalStateException("processId and prosessSteg should always be null before execute ");
        }

        threadLocalAccessInfo.saksbehandler = saksbehandler;
        threadLocalAccessInfo.processId = processId;
        threadLocalAccessInfo.prosessSteg = prosessSteg;
    }


    public static void afterExecuteProcess(UUID processId) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.processId != processId) {
            throw new IllegalStateException("start and end processId should be equal \n"
                + threadLocalAccessInfo.processId + " != " + processId.toString());
        }
        threadLocalStorage.remove();
    }

    public static boolean shouldUseOidcToken() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.isFromWebRequest();
    }

    public static boolean shouldUseSystemToken() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.isFromProcess()) {
            return true;
        }

        if (!threadLocalAccessInfo.isFromWebRequest()) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            var stackTraceElements = Arrays.stream(stackTrace).map(StackTraceElement::toString).toList();
            String stackTraceAsString = String.join("\n", stackTraceElements);
            log.warn("Call have not been registret from RestController or Prosess\n{}", stackTraceAsString);
            return true;
        }
        return threadLocalAccessInfo.isFromAdminRequest();
    }

    public static String getInfo() {
        return threadLocalStorage.get().toString();
    }

    @Override
    public String toString() {
        return "ThreadLocalAccessInfo{" +
            "requestUri='" + requestUri + '\'' +
            ", prossessId='" + processId + '\'' +
            '}';
    }
}
