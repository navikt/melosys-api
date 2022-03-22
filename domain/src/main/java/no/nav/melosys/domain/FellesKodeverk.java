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
    LANDKODER_ISO2("LandkoderISO2"),
    LOVVALG_MEDL("LovvalgMedl"),
    PERIODESTATUS_MEDL("PeriodestatusMedl"),
    PERIODETYPE_MEDL("PeriodetypeMedl"),
    PERMISJONS_OG_PERMITTERINGS_BESKRIVELSE("PermisjonsOgPermitteringsBeskrivelse"),
    PERSONSTATUSER("Personstatuser"),
    POSTNUMMER("Postnummer"),
    SKIPSREGISTRE("Skipsregistre"),
    SKIPSTYPER("Skipstyper"),
    SIVILSTANDER("Sivilstander"),
    STATSBORGERSKAP_FREG("StatsborgerskapFreg"),
    YRKER("Yrker");

    FellesKodeverk(String navn) {
        this.navn = navn;
    }

    private final String navn;

    public String getNavn() {
        return navn;
    }

}
