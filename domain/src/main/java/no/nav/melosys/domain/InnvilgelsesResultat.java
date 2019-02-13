package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum InnvilgelsesResultat implements Kodeverk {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLAATT("AVSLAATT", "Avslått");

    private String kode;
    private String beskrivelse;

    InnvilgelsesResultat(String kode, String beskrivelse) {
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
