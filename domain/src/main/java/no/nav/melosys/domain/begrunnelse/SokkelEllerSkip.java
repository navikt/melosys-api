package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum SokkelEllerSkip implements Kodeverk {

    BORESKIP("BORESKIP", "Boreskip - Sokkel"),
    FAST_INSTALLASJON("FAST_INSTALLASJON", "Fast installasjon"),
    IKKE_EGEN_FREMDRIFT("IKKE_EGEN_FREMDRIFT", "Ikke framdrift for egen maskin"),
    IKKE_ORDINAER_SKIPSFART("IKKE_ORDINAER_SKIPSFART", "Ikke skrogtype som kan brukes i ordinær skipsfart");

    private String kode;
    private String beskrivelse;

    SokkelEllerSkip(String kode, String beskrivelse) {
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

