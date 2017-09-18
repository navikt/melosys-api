package no.nav.melosys.domain.dokument;

/**
 * Felles interface for kodeverk som hardkodes.
 * 
 * (Kodeverk som forretningsregler eller -logikk må forholde seg til blir hardkodet som enums.)
 */
public interface KodeverkEnum {

    public String getNavn();
    
}
