package no.nav.melosys.domain.arkiv;

/*
 * Til bruk for journalposter der hoveddokument er ett scannet dokument som inneholder både hoveddokument og vedlegg
 */
public class LogiskeVedlegg {
    private String tittel;

    public LogiskeVedlegg(String tittel) {
        this.tittel = tittel;
    }

    public String getTittel() {
        return tittel;
    }
}
