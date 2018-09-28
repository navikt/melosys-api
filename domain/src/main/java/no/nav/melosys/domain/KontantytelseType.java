package no.nav.melosys.domain;

public enum KontantytelseType implements Kodeverk {

    FP("FP", "Foreldrepenger"),
    SP("SP", "Sykepenger"),
    AAP("AAP", "Arbeidsavklaringspenger"),
    DP("DP", "Dagpenger");

    private String kode;
    private String beskrivelse;

    KontantytelseType(String kode, String beskrivelse) {
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
