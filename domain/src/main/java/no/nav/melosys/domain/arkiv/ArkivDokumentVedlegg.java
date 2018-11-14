package no.nav.melosys.domain.arkiv;

/*
 * Til bruk for journalposter der hoveddokument er ett scannet dokument som inneholder både hoveddokument og vedlegg
 */
public class ArkivDokumentVedlegg {
    private String tittel;

    public ArkivDokumentVedlegg(String tittel) {
        this.tittel = tittel;
    }
}
