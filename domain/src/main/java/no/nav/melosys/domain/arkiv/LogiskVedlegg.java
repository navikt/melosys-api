package no.nav.melosys.domain.arkiv;

/*
 * Til bruk for journalposter der hoveddokument er ett scannet dokument som inneholder både hoveddokument og vedlegg
 */
public class LogiskVedlegg {
    private final String logiskVedleggID;
    private final String tittel;

    public LogiskVedlegg(String logiskVedleggID, String tittel) {
        this.logiskVedleggID = logiskVedleggID;
        this.tittel = tittel;
    }

    public String getLogiskVedleggID() {
        return logiskVedleggID;
    }

    public String getTittel() {
        return tittel;
    }
}
