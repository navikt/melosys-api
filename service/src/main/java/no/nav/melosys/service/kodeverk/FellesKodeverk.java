package no.nav.melosys.service.kodeverk;

public enum FellesKodeverk {
    
    ARBEIDSTIDSORDNINGER("Arbeidstidsordninger"),
    ARBEIDSFORHOLDSTYPER("Arbeidsforholdstyper"),
    DEKNING_MEDL("DekningMedl"),
    DISKRESJONSKODER("Diskresjonskoder"),
    DOKUMENTKATEGORIER("Dokumentkategorier"),
    GRUNNLAG_MEDL("GrunnlagMedl"),
    KILDEDOKUMENT_MEDL("KildedokumentMedl"),
    KILDESYSTEM_MEDL("KildesystemMedl"),
    KJØNNSTYPER("Kjønnstyper"),
    LANDKODER("Landkoder"),
    LANDKODERISO2("LandkoderISO2"),
    LOVVALG_MEDL("LovvalgMedl"),
    PERIODESTATUS_MEDL("PeriodestatusMedl"),
    POSTNUMMER("Postnummer"),
    SIVILSTANDER("Sivilstander");

    private FellesKodeverk(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    public String getNavn() {
        return navn;
    }

}
