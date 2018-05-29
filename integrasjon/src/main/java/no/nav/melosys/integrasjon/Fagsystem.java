package no.nav.melosys.integrasjon;

import no.nav.melosys.domain.Kodeverk;

public enum Fagsystem implements Kodeverk {

    GSAK("FS19", "GSAK"),
    MELOSYS("FS38", "Melosys"); // "https://confluence.adeo.no/display/APPKAT/Applikasjons-ID"

    private String kode;
    private String beskrivelse;

    private Fagsystem(String kode, String beskrivelse) {
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
