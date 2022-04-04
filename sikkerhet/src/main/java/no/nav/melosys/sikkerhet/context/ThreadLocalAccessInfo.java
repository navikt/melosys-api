package no.nav.melosys.sikkerhet.context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalAccessInfo {
    private String requestUri;
    private UUID processId;
    private String prosessSteg;

    public static final Map<String, Integer> debugInfoUsage = new ConcurrentHashMap<>(); // For debug only - will be removed
    public static final Map<String, Integer> debugInfoChecks = new ConcurrentHashMap<>(); // For debug only - will be removed

    private boolean isFromWebRequest() {
        return requestUri != null;
    }

    private boolean isFromProcess() {
        return processId != null;
    }

    private static final ThreadLocal<ThreadLocalAccessInfo> threadLocalStorage =
        ThreadLocal.withInitial(ThreadLocalAccessInfo::new);

    public static void beforeControllerRequest(String requestUri) {
        increaseCount(debugInfoUsage, "web"); // For debug only - will be removed
        if (threadLocalStorage.get().requestUri != null) {
            throw new IllegalStateException("We should not have a thread local requestUri before controller request");
        }
        threadLocalStorage.get().requestUri = requestUri;
    }

    public static void afterControllerRequest(String requestUri) {
        if (!threadLocalStorage.get().requestUri.equals(requestUri)) {
            throw new IllegalStateException("start and end request should be equal \n"
                + threadLocalStorage.get().requestUri + " != " + requestUri);
        }
        threadLocalStorage.remove();
    }

    public static void beforExecuteProcess(UUID processId, String prosessSteg) {
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

    public static boolean isFrontendCall() {
        increaseCount(debugInfoChecks, "web"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.isFromWebRequest()
            && !threadLocalAccessInfo.requestUri.equals("/admin/prosessinstanser/restart");
    }

    public static boolean isProcessCall() {
        increaseCount(debugInfoChecks, "process"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalStorage.get();
        return threadLocalAccessInfo.isFromProcess();
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
