package no.nav.melosys.sikkerhet.context;

public class SaksflytSubjektHolder {
    private SaksflytSubjektHolder() {
    }

    private static final ThreadLocal<String> SAKSFLYT_DATA = new ThreadLocal<>();

    public static String get() {
        return SAKSFLYT_DATA.get();
    }

    public static void reset() {
        SAKSFLYT_DATA.remove();
    }

    public static void set(String subjekt) {
        SAKSFLYT_DATA.set(subjekt);
    }
}
