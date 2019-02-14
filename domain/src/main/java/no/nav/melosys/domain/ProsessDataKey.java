package no.nav.melosys.domain;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    ARBEIDSGIVER("arbeidsgiver"),
    ARKIV_ID("arkivId"),
    ARKIVSYSTEM("arkivsystem"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_NAVN("avsenderNavn"),
    BEGRUNNELSEKODE("begrunnelsekode"),
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEHANDLINGSTEMA("behandlingstema"),
    BEHANDLINGSTYPE("behandlingstype"),
    BREVDATA("brevData"),
    BRUKER_ID("brukerID"),
    DOKUMENT_ID("dokumentID"),
    FRITEKST("fritekst"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JFR_INGEN_VURDERING("ingenVurdering"),
    JOURNALPOST_ID("journalpostID"),
    OPPHOLDSLAND("land"),
    OPPGAVE_ID("oppgaveID"),
    REPRESENTANT("representant"),
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSNUMMER("saksnummer"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema");

    private String kode;

    private static final Map<String, ProsessDataKey> KEYS;

    static {
        Map<String, ProsessDataKey> map = new ConcurrentHashMap<>();
        for (ProsessDataKey key : ProsessDataKey.values()) {
            map.put(key.getKode(), key);
        }
        KEYS = Collections.unmodifiableMap(map);
    }

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static ProsessDataKey fraKode(String kode) {
        return KEYS.get(kode);
    }
}
