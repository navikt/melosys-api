package no.nav.melosys.service.kodeverk;

public enum Kodeverk {
    
    ARBEIDSTIDSORDNINGER("Arbeidstidsordninger"),
    ARBEIDSFORHOLDSTYPER("Arbeidsforholdstyper"),
    POSTNUMMER("Postnummer"),
    LANDKODER("Landkoder"),
    LANDKODERISO2("LandkoderISO2"),
    SIVILSTANDER("Sivilstander");
    
    private Kodeverk(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    public String getNavn() {
        return navn;
    }

}
