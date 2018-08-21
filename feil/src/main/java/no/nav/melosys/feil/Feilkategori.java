package no.nav.melosys.feil;

/**
 * Støtte for feilhåndteringsstrategier.
 */
public enum Feilkategori {
    
    // Tekniske feil:
    TEKNISK_FEIL,
    INTEGRASJON_FEIL,
    UVENTET_EXCEPTION,
    
    // Funksjonelle feil:
    FUNKSJONELL_FEIL,
    INGEN_TILGANG,
    IKKE_FUNNET
    
}
