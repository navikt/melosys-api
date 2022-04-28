package no.nav.melosys.sikkerhet.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalAccessInfo {
    private static final Logger log = LoggerFactory.getLogger(ThreadLocalAccessInfo.class);
    private String requestUri;
    private UUID processId;
    private String prosessSteg;
    private boolean isAdminRequest;

    public static final Map<String, Integer> debugInfoUsage = new ConcurrentHashMap<>(); // For debug only - will be removed
    public static final Map<String, Integer> debugInfoChecks = new ConcurrentHashMap<>(); // For debug only - will be removed

    private boolean isFromWebRequest() {
        return requestUri != null;
    }

    private boolean isFromProcess() {
        return processId != null;
    }

    private boolean isFromAdminRequest() {
        return isAdminRequest;
    }

    private static final ThreadLocal<ThreadLocalAccessInfo> threadLocalStorage =
        ThreadLocal.withInitial(ThreadLocalAccessInfo::new);

    public static void beforeControllerRequest(String requestUri, boolean isAdminRequest) {
        increaseCount(debugInfoUsage, "web, admin:" + isAdminRequest ); // For debug only - will be removed
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

    public static void beforeExecuteProcess(UUID processId, String prosessSteg) {
        increaseCount(debugInfoUsage, "process"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.processId != null || threadLocalAccessInfo.prosessSteg != null) {
            throw new IllegalStateException("processId and prosessSteg should always be null before execute ");
        }

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

    public static boolean useOicdToken() {
        increaseCount(debugInfoChecks, "web"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.isFromWebRequest();
    }

    public static boolean useSystemToken() {
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        if (threadLocalAccessInfo.isFromProcess()) {
            increaseCount(debugInfoChecks, "process"); // For debug only - will be removed
            return true;
        }

        if (!threadLocalAccessInfo.isFromWebRequest()) {
            increaseCount(debugInfoChecks, "unknown"); // For debug only - will be removed
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

    private static void increaseCount(Map<String, Integer> map, String source) {
        Integer cnt = map.getOrDefault(source, 0);
        map.put(source, cnt + 1);
    }
}
