package no.nav.melosys.domain;

public enum FellesKodeverk {
    
    ARBEIDSTIDSORDNINGER("Arbeidstidsordninger"),
    DEKNING_MEDL("DekningMedl"),
    DISKRESJONSKODER("Diskresjonskoder"),
    DOKUMENTKATEGORIER("Dokumentkategorier"),
    ENHETSTYPER_JURIDISK_ENHET("EnhetstyperJuridiskEnhet"),
    FAMILIERELASJONER("Familierelasjoner"),
    FARTSOMRAADE("Fartsområder"),
    GRUNNLAG_MEDL("GrunnlagMedl"),
    KILDEDOKUMENT_MEDL("KildedokumentMedl"),
    KILDESYSTEM_MEDL("KildesystemMedl"),
    KJØNNSTYPER("Kjønnstyper"),
    LANDKODER("Landkoder"),
    LANDKODERISO2("LandkoderISO2"),
    LOVVALG_MEDL("LovvalgMedl"),
    PERIODESTATUS_MEDL("PeriodestatusMedl"),
    PERIODETYPE_MEDL("PeriodetypeMedl"),
    PERSONSTATUSER("Personstatuser"),
    POSTNUMMER("Postnummer"),
    SKIPSREGISTRE("Skipsregistre"),
    SKIPSTYPER("Skipstyper"),
    SIVILSTANDER("Sivilstander");

    FellesKodeverk(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    public String getNavn() {
        return navn;
    }

}
