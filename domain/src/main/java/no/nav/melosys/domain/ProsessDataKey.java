package no.nav.melosys.domain;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_NAVN("avsenderNavn"),
    BRUKER_ID("brukerID"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JOURNALPOST_ID("journalpostID"),
    OPPGAVE_ID("oppgaveID"),
    SAKSNUMMER("saksnummer");

    private String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
