package no.nav.melosys.domain;

public enum FellesKodeverk {
    
    ARBEIDSTIDSORDNINGER("Arbeidstidsordninger"),
    DEKNING_MEDL("DekningMedl"),
    DOKUMENTKATEGORIER("Dokumentkategorier"),
    ENHETSTYPER_JURIDISK_ENHET("EnhetstyperJuridiskEnhet"),
    GRUNNLAG_MEDL("GrunnlagMedl"),
    KILDEDOKUMENT_MEDL("KildedokumentMedl"),
    KILDESYSTEM_MEDL("KildesystemMedl"),
    KJØNNSTYPER("Kjønnstyper"),
    LANDKODER("Landkoder"),
    LANDKODERISO2("LandkoderISO2"),
    LOVVALG_MEDL("LovvalgMedl"),
    PERIODESTATUS_MEDL("PeriodestatusMedl"),
    POSTNUMMER("Postnummer");

    FellesKodeverk(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    public String getNavn() {
        return navn;
    }

}
