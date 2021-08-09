package no.nav.melosys.domain.arkiv;

/*
 * Til bruk for journalposter der hoveddokument er ett scannet dokument som inneholder både hoveddokument og vedlegg
 */
public record LogiskVedlegg (String logiskVedleggID, String tittel){}
