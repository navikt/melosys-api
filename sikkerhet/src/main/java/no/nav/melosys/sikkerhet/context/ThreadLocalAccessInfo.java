package no.nav.melosys.sikkerhet.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalAccessInfo {
    private final Logger log = LoggerFactory.getLogger(ThreadLocalAccessInfo.class);

    private String requestUri;
    private UUID processId;
    private String prosessSteg;

    private final static Map<String, String> debugInfoRequest = new ConcurrentHashMap<>();
    private final static Map<String, String> debugInfoProcess = new ConcurrentHashMap<>();
    private final static Map<String, String> debugInfoBoth = new ConcurrentHashMap<>();
    public final static Map<String, String> debugWarnFront = new ConcurrentHashMap<>();
    public final static Map<String, String> debugWarnProcess = new ConcurrentHashMap<>();

    public boolean isFromRequest() {
        return requestUri != null;
    }

    public boolean isFromProcess() {
        return processId != null;
    }

    private final static ThreadLocal<ThreadLocalAccessInfo> threadLocalAccessInfo =
        ThreadLocal.withInitial(ThreadLocalAccessInfo::new);

    public static void fromContextExchangeFilter(String clientRequest) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        if (threadLocalAccessInfo.isFromRequest()) {
            debugInfoRequest.put(threadLocalAccessInfo.requestUri, clientRequest);
        }
        if (threadLocalAccessInfo.isFromProcess()) {
            debugInfoProcess.put(threadLocalAccessInfo.processId.toString(), clientRequest);
        }
        if (threadLocalAccessInfo.isFromRequest() && threadLocalAccessInfo.isFromProcess()) {
            debugInfoBoth.put(threadLocalAccessInfo.prosessSteg + " - " + threadLocalAccessInfo.requestUri,
                clientRequest);
        }
    }

    public static void preHandle(String requestUri) {
        if (threadLocalAccessInfo.get().requestUri != null) {
            throw new IllegalStateException("We should not have a thread local requestUri before controller request");
        }
        threadLocalAccessInfo.get().requestUri = requestUri;
    }

    public static void afterCompletion(String requestUri) {
        if (!threadLocalAccessInfo.get().requestUri.equals(requestUri)) {
            throw new IllegalStateException("start and end request should be equal \n"
                + threadLocalAccessInfo.get().requestUri + " != " + requestUri);
        }
        threadLocalAccessInfo.get().requestUri = null;
    }

    public static void beforExecuteProcess(UUID processId, String prosessSteg) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        if (threadLocalAccessInfo.processId != null || threadLocalAccessInfo.prosessSteg != null) {
            throw new IllegalStateException("processId and prosessSteg should always be null before execute ");
        }

        threadLocalAccessInfo.processId = processId;
        threadLocalAccessInfo.prosessSteg = prosessSteg;
    }

    public static void afterExecuteProcess(UUID processId) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        assert threadLocalAccessInfo.processId == processId;
        threadLocalAccessInfo.processId = null;
        threadLocalAccessInfo.prosessSteg = null;
    }

    public static boolean isFrontendCall() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        return threadLocalAccessInfo.isFromRequest()
            && !threadLocalAccessInfo.requestUri.equals("/admin/prosessinstanser/restart");
    }

    public static void warnFrontendCall(Object object, String externalServiceUrl) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        debugWarnFront.put(object.getClass().getSimpleName() + "-" + threadLocalAccessInfo.toString(), externalServiceUrl);
    }

    public static void warnProcessCall(String externalServiceUrl) {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        debugWarnProcess.put(threadLocalAccessInfo.toString(), externalServiceUrl);
    }

    public static boolean isProcessCall() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        return threadLocalAccessInfo.isFromProcess();
    }

    public static String getInfo() {
        return threadLocalAccessInfo.get().toString();
    }

    public static Map<String, String> requestWithExtrernalCalls() {
        return debugInfoRequest;
    }

    public static Map<String, String> processesWithExtrernalCalls() {
        return debugInfoProcess;
    }

    public static Map<String, String> bothWithExtrernalCalls() {
        return debugInfoBoth;
    }

    @Override
    public String toString() {
        return "ThreadLocalAccessInfo{" +
            "requestUri='" + requestUri + '\'' +
            ", prossessId='" + processId + '\'' +
            '}';
    }
}
