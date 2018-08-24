package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;

public enum Diskresjonskode implements KodeverkHjelper {

    MILI("MILI"),
    UFB("UFB"),
    URIK("URIK"),
    SPSF("SPSF"),
    SVAL("SVAL"),
    SPFO("SPFO"),
    PEND("PEND"),
    KLIE("KLIE");
    
    private String kode;
    
    Diskresjonskode(String kode) {
        this.kode = kode;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.DISKRESJONSKODER;
    }
}
