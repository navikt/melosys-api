package no.nav.melosys.domain.dokument.arbeidsforhold;

import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.domain.util.FellesKodeverk;

public class Arbeidstidsordning implements KodeverkHjelper {
    public static final String doegnkontinuerligSkiftOgTurnus355 = "doegnkontinuerligSkiftOgTurnus355";
    public static final String helkontinuerligSkiftOgAndreOrdninger336 = "helkontinuerligSkiftOgAndreOrdninger336";
    public static final String ikkeSkift = "ikkeSkift";
    public static final String offshore336 = "offshore336";
    public static final String skift365 = "skift365";

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
