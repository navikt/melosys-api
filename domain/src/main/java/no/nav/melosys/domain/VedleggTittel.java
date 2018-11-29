package no.nav.melosys.domain;

public enum  VedleggTittel implements Kodeverk {

    ARBF("ARBF", "Arbeidsforhold"),
    BKR_MEDL("BKR_MEDL", "Bekreftelse på medlemskap i folketrygden"),
    INNT_SKAT("INNT_SKAT", "Inntektsopplysninger"),
    MERK("MERK", "Merknad til sak"),
    STUDIE_DOKUMENTASJON("STUDIE_DOKUMENTASJON", "Studiedokumentasjon"),
    SOK_MED("SOK_MED", "Søknad om medlemskap"),
    BEKR_UNNT_FRA_MEDL("BEKR_UNNT_FRA_MEDL", "Unntak"),
    ANNET("ANNET", "Annet (=fritekst)");

    private String kode;
    private String beskrivelse;

    private VedleggTittel(String kode, String beskrivelse) {
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
