package no.nav.melosys.domain.dokument.arbeidsforhold;

import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.domain.FellesKodeverk;

public class Arbeidstidsordning implements KodeverkHjelper {

    private String kode;

    // Brukes av JAXB
    public Arbeidstidsordning() {}

    public Arbeidstidsordning(String arbeidstidsordning) {
        this.kode = arbeidstidsordning;
    }

    public String getKode() {
        return kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.ARBEIDSTIDSORDNINGER;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

}
