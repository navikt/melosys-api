package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket Personstatuser.
 */
public enum Personstatus implements KodeverkEnum<Personstatus> {

    ADNR("Aktivt"),
    UTPE("Utgått person"),
    BOSA("Bosatt"),
    UREG("Uregistrert person"),
    ABNR("Aktivt BOSTNR"),
    UFUL("Ufullstendig fødselsnr"),
    UTVA("Utvandret"),
    FOSV("Forsvunnet/savnet"),
    DØDD("Død"),
    DØD("Død"),
    UTAN("Utgått person annullert tilgang Fnr"),
    FØDR("Fødselregistrert");
    
    private String navn;
    
    private Personstatus(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }

}