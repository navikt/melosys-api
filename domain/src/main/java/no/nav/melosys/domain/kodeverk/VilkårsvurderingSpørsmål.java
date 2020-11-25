package no.nav.melosys.domain.kodeverk;

public enum VilkårsvurderingSpørsmål implements Kodeverk {
    FTRL_2_8_FORUTGÅENDE_TRYGDETID("Medlem i minst tre av de fem siste kalenderårene"),
    FTRL_2_8_NÆR_TILKNYTNING_NORGE("Nær tilknytning til det norske samfunnet");

    private final String beskrivelse;

    VilkårsvurderingSpørsmål(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return this.name();
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
