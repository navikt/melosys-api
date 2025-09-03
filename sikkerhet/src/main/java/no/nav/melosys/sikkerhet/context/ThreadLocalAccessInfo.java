package no.nav.melosys.sikkerhet.context;

import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalAccessInfo {
    private static final Logger log = LoggerFactory.getLogger(ThreadLocalAccessInfo.class);
    private String requestUri;
    private UUID processId;
    private String processName;
    private boolean isAdminRequest;

    private String saksbehandler;

    private String saksbehandlerNavn;

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

    public static String getSaksbehandlerNavn() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.saksbehandlerNavn;
    }

    public static UUID getProcessId() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.processId;
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
        log.debug("After a controller request:  {}", threadLocalStorage.get());

        if (!threadLocalStorage.get().requestUri.equals(requestUri)) {
            throw new IllegalStateException("start and end request should be equal \n"
                + threadLocalStorage.get().requestUri + " != " + requestUri);
        }
        threadLocalStorage.remove();
    }

    public static void executeProcess(String processName, Runnable runnable) {
        UUID processId = UUID.randomUUID();
        executeProcess(processId, processName, runnable);
    }

    public static void executeProcess(UUID processId, String processName, Runnable runnable) {
        beforeExecuteProcess(processId, processName);
        try {
            runnable.run();
        } finally {
            afterExecuteProcess(processId);
        }
    }

    public static void beforeExecuteProcess(UUID processId, String processName) {
        beforeExecuteProcess(processId, processName, null, null);
    }

    public static void beforeExecuteProcess(UUID processId, String processName, String saksbehandler, String saksbehandlerNavn) {
        log.info("Before process {}: {}", processId, ThreadLocalAccessInfo.threadLocalStorage.get());

        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.processId != null || threadLocalAccessInfo.processName != null) {
            throw new IllegalStateException("processId and processName should always be null before execute ");
        }

        threadLocalAccessInfo.saksbehandler = saksbehandler;
        threadLocalAccessInfo.saksbehandlerNavn = saksbehandlerNavn;
        threadLocalAccessInfo.processId = processId;
        threadLocalAccessInfo.processName = processName;
    }


    public static void afterExecuteProcess(UUID processId) {
        log.info("After process {}: {}", processId, ThreadLocalAccessInfo.threadLocalStorage.get());

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
            log.warn("Call have not been registrert from RestController or Prosess\n{}", stackTraceAsString);
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
            "isAdminRequest=" + isAdminRequest +
            ", processName='" + processName + '\'' +
            ", processId=" + processId +
            ", requestUri='" + requestUri + '\'' +
            '}';
    }
}
