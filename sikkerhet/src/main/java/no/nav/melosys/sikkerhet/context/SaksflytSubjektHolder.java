package no.nav.melosys.sikkerhet.context;

public class SaksflytSubjektHolder {
    private SaksflytSubjektHolder() {}

    private static final ThreadLocal<String> saksflytData = new ThreadLocal<>();

    public static String get() {
        return saksflytData.get();
    }

    public static void set(String subjekt) {
        saksflytData.set(subjekt);
    }
}
