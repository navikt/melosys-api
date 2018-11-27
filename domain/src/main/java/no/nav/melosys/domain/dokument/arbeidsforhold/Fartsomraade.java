package no.nav.melosys.domain.dokument.arbeidsforhold;

import javax.xml.bind.annotation.XmlEnumValue;

import no.nav.melosys.domain.Kodeverk;

public enum Fartsomraade implements Kodeverk {
    @XmlEnumValue("innenriks")
    INNENRIKS("INNENRIKS", "Innenriks"),
    @XmlEnumValue("utenriks")
    UTENRIKS("UTENRIKS", "Utenriks");

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
