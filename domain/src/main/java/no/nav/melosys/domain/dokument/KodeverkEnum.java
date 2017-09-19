package no.nav.melosys.domain.dokument;

/**
 * Felles interface for kodeverk som hardkodes.
 * 
 * IKKE implementer denne klassen for kodeverk med mindre forretningsregler eller -logikk må forholde seg til kodeverket. 
 * 
 * Altså: Kodeverk som ikke har innvirkning på utfall (f.eks. kjønn), skal lagres som String (bruk KodeverkService til å gjøre oppslag).
 * 
 */
public interface KodeverkEnum<E extends KodeverkEnum<E>> {

    public String getNavn();
    
}
