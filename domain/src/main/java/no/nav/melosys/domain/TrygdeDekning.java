package no.nav.melosys.domain;

public enum TrygdeDekning implements Kodeverk {

    FULL_DEKNING_EOSFO("FULL_DEKNING_EOSFO", "Full medlemskap i trygden for ytelser omfattet av EU/EØS forordningen."),
    UTEN_DEKNING("UTEN_DEKNING", "Ingen rettigheter i trygden.");
    
    private String kode;
    private String beskrivelse;

    TrygdeDekning(String kode, String beskrivelse) {
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
