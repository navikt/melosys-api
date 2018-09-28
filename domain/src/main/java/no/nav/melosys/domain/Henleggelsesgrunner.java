package no.nav.melosys.domain;

public enum Henleggelsesgrunner implements Kodeverk {

    ANNET("ANNET", "Annet"),
    OPPHOLD_UTL_AVLYST("OPPHOLD_UTL_AVLYST", "Utlandsopphold avlyst"),
    SØKNADEN_TRUKKET("SØKNADEN_TRUKKET", "Søknaden trukket");

    private String kode;
    private String beskrivelse;

    Henleggelsesgrunner(String kode, String beskrivelse) {
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
