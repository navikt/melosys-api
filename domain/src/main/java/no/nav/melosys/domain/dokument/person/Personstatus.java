package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;

public enum Personstatus implements KodeverkHjelper {

    ADNR("ADNR"),
    UTPE("UTPE"),
    BOSA("BOSA"),
    UREG("UREG"),
    ABNR("ABNR"),
    UFUL("UFUL"),
    UTVA("UTVA"),
    FOSV("FOSV"),
    DØDD("DØDD"),
    DØD("DØD"),
    UTAN("UTAN"),
    FØDR("FØDR");
    
    private String kode;
    
    Personstatus(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.PERSONSTATUSER;
    }

}