package no.nav.melosys.integrasjon;

import no.nav.melosys.domain.Kodeverk;

public enum Fagsystem implements Kodeverk {

    GSAK("FS19", "GSAK"),
    MELOSYS("TODO", "Melosys"); //FIXME Hvem definerer koden for Melosys?

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
