package no.nav.melosys.domain.dokument.arbeidsforhold;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum Fartsomraade implements Kodeverk {


    INNENRIKS("innenriks", "Innenriks"),

    UTENRIKS("utenriks", "Utenriks");

    private String kode;
    private String beskrivelse;

    Fartsomraade(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
