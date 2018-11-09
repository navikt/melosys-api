package no.nav.melosys.domain.arkiv;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Journalposttype {
    /**
     * Inngående dokument
     */
    INN("I"),
    /**
     * Utgående dokument
     */
    UT("U"),
    /**
     * Internt notat
     */
    NOTAT("N");

    private String kode;

    private static final Map<String, Journalposttype> JOURNALPOSTTYPER;

    static {
        Map<String, Journalposttype> map = new ConcurrentHashMap<>();
        for (Journalposttype journalposttype : Journalposttype.values()) {
            map.put(journalposttype.getKode(), journalposttype);
        }
        JOURNALPOSTTYPER = Collections.unmodifiableMap(map);
    }

    Journalposttype(String kode) {
        this.kode = kode;
    }

    public static Journalposttype fraKode(String kode) {
        return JOURNALPOSTTYPER.get(kode);
    }

    public String getKode() {
        return kode;
    }
}
