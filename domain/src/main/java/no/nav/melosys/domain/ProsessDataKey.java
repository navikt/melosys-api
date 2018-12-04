package no.nav.melosys.domain;

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
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEHANDLINGSTEMA("behandlingstema"),
    BREVDATA("brevData"),
    BRUKER_ID("brukerID"),
    DOKUMENT_ID("dokumentID"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JOURNALPOST_ID("journalpostID"),
    OPPHOLDSLAND("land"),
    OPPGAVE_ID("oppgaveID"),
    REPRESENTANT("representant"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSNUMMER("saksnummer"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema");

    private String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
