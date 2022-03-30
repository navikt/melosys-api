package no.nav.melosys.sikkerhet.context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalAccessInfo {
    private String requestUri;
    private UUID processId;
    private String prosessSteg;

    public final static Map<String, Integer> debugInfoUsage = new ConcurrentHashMap<>(); // For debug only - will be removed
    public final static Map<String, Integer> debugInfoChecks = new ConcurrentHashMap<>(); // For debug only - will be removed

    public boolean isFromRequest() {
        return requestUri != null;
    }

    public boolean isFromProcess() {
        return processId != null;
    }

    private final static ThreadLocal<ThreadLocalAccessInfo> threadLocalAccessInfo =
        ThreadLocal.withInitial(ThreadLocalAccessInfo::new);

    public static void beforeControllerRequest(String requestUri) {
        increaseCount(debugInfoUsage, "web"); // For debug only - will be removed
        if (threadLocalAccessInfo.get().requestUri != null) {
            throw new IllegalStateException("We should not have a thread local requestUri before controller request");
        }
        threadLocalAccessInfo.get().requestUri = requestUri;
    }

    public static void afterControllerRequest(String requestUri) {
        if (!threadLocalAccessInfo.get().requestUri.equals(requestUri)) {
            throw new IllegalStateException("start and end request should be equal \n"
                + threadLocalAccessInfo.get().requestUri + " != " + requestUri);
        }
        threadLocalAccessInfo.get().requestUri = null;
    }

    public static void beforExecuteProcess(UUID processId, String prosessSteg) {
        increaseCount(debugInfoUsage, "process"); // For debug only - will be removed
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
        increaseCount(debugInfoChecks, "web"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        return threadLocalAccessInfo.isFromRequest()
            && !threadLocalAccessInfo.requestUri.equals("/admin/prosessinstanser/restart");
    }

    public static boolean isProcessCall() {
        increaseCount(debugInfoChecks, "process"); // For debug only - will be removed
        ThreadLocalAccessInfo threadLocalAccessInfo = ThreadLocalAccessInfo.threadLocalAccessInfo.get();
        return threadLocalAccessInfo.isFromProcess();
    }

    public static String getInfo() {
        return threadLocalAccessInfo.get().toString();
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
