package no.nav.melosys.domain;

public enum DokumentTittel implements Kodeverk {

    ARBEIDSFORHOLD("ARBF", "Arbeidsforhold"),
    BEKREFTELSE_MEDLEMSSKAP("BKR_MEDL", "Bekreftelse på medlemskap i folketrygden"),
    INNTEKT_SKAT("INNT_SKAT", "Inntektsopplysninger"),
    MERKNAD_TIL_SAK("MERK", "Merknad til sak"),
    STUDIEDOKUMENTASJON("STUDIE_DOKUMENTASJON", "Studiedokumentasjon"),
    SØKNAD_MEDLEMSSKAP("SOK_MED", "Søknad om medlemskap"),
    BEKREFTELSE_UNNTAK("BEKR_UNNT_FRA_MEDL", "Unntak"),
    ANNET("ANNET", "Annet (=fritekst)");

    private String kode;
    private String beskrivelse;

    DokumentTittel(String kode, String beskrivelse) {
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
