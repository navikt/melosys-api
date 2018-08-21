package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.domain.util.FellesKodeverk;

public class KjønnsType implements KodeverkHjelper{

    public static final String K = "K";
    public static final String M = "M";
    public static final String U = "U";

    private String kode;

    // Brukes av JAXB
    public KjønnsType() {}

    public KjønnsType(String kjønnstypeKode) {
        this.kode = kjønnstypeKode;
    }

    public String getKode() {
        return kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.KJØNNSTYPER;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

}
