package no.nav.melosys.domain;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    ARKIV_ID("arkivId"),
    ARKIVSYSTEM("arkivsystem"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_NAVN("avsenderNavn"),
    BEHANDLINGSTEMA("behandlingstema"),
    BRUKER_ID("brukerID"),
    DOKUMENT_ID("dokumentID"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JOURNALPOST_ID("journalpostID"),
    LAND("land"),
    OPPGAVE_ID("oppgaveID"),
    SAKSNUMMER("saksnummer"),
    SOB_BEHANDLING_ID("sobBehandlingId"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema"),
    ARBEIDSGIVER("arbeidsgiver"),
    REPRESENTANT("representant");

    private String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
