package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.domain.FellesKodeverk;

public class KjoennsType implements KodeverkHjelper {

    private String kode;

    // Brukes av JAXB
    public KjoennsType() {}

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
