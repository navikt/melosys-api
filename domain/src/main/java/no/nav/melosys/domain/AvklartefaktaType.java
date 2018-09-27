package no.nav.melosys.domain;

public enum AvklartefaktaType implements Kodeverk {

    AG_FORRETNINGSLAND("AG_FORRETNINGSLAND", "Arbeidsgivers forretningsland"),
    MOTTAR_KONTANTYTELSE("MOTTAR_KONTANTYTELSE", "Mottar kontantytelse"),
    OFFENTLIG_TJENESTEMANN("OFFENTLIG_TJENESTEMANN", "Offentlig tjenestemann"),
    BOSTEDSLAND("BOSTEDSLAND", "Bostedsland"),
    SOKKEL_SKIP("SOKKEL_SKIP", "Arbeid på sokkel eller skip"),;

    private String kode;
    private String beskrivelse;

    AvklartefaktaType(String kode, String beskrivelse) {
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
