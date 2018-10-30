package no.nav.melosys.domain;

public enum Behandlingsmaate implements Kodeverk {

    AUTOMATISERT("AUTOMATISERT", "Automatisert"),
    MANUELT("MANUELT", "Manuelt"),
    DELVIS_AUTOMATISERT("DELVIS_AUTOMATISERT", "Delvis automatisert"),
    UDEFINERT("UDEFINERT", "Udefinert");


    private String kode;
    private String beskrivelse;

    Behandlingsmaate(String kode, String beskrivelse) {
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
