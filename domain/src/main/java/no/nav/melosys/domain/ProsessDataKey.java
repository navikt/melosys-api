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
    BEHANDLINGSRESULTAT_BEGRUNNELSER("behandlingsresultatBegrunnelse"),
    BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST("behandlingsresultatBegrunnelseFritekst"),
    BEHANDLINGSTEMA("behandlingstema"),
    BEHANDLINGSTYPE("behandlingstype"),
    BREVDATA("brevData"),
    BRUKER_ID("brukerID"),
    DOKUMENT_ID("dokumentID"),
    EESSI_MELDING("eessiMelding"),
    ER_ENDRING("erEndring"),
    FRITEKST("fritekst"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JFR_INGEN_VURDERING("ingenVurdering"),
    JOURNALPOST_ID("journalpostID"),
    MOTTAKER("mottaker"),
    OPPHOLDSLAND("oppholdsland"),
    OPPGAVE_ID("oppgaveID"),
    REPRESENTANT("representant"),
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSNUMMER("saksnummer"),
    SED_DOKUMENT("sedDokument"),
    SKAL_TILORDNES("skalTilordnes"),
    STATSBORGERSKAP("statsborgerskap"),
    SØKNADSLAND("land"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema"),
    VEDLEGG_TITTEL_LISTE("vedleggTittelListe");

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
