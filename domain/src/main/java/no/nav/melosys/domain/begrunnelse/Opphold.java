package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum Opphold implements Kodeverk {

    FEIL_LAND_JOURNALFOERING("FEIL_LAND_JOURNALFOERING", "Feil land oppgitt i journalføringen."),
    UGYLDIG_TERRITORIE("UGYLDIG_TERRITORIE", "Søker skal til et territorium som ikke er en del av forordningen."),
    NYE_OPPLYSNINGER("NYE_OPPLYSNINGER", "Nye opplysninger om arbeids/oppholdsland.");

    private String kode;
    private String beskrivelse;

    Opphold(String kode, String beskrivelse) {
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
