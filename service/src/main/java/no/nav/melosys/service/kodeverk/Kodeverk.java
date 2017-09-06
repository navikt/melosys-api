package no.nav.melosys.service.kodeverk;

public enum Kodeverk {
    
    ARBEIDSTIDSORDNINGER("Arbeidstidsordninger"),
    ARBEIDSFORHOLDSTYPER("Arbeidsforholdstyper"),
    POSTNUMMER("Postnummer"),
    LANDKODER("Landkoder"),
    SIVILSTANDER("Sivilstander");
    
    private Kodeverk(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    String getNavn() {
        return navn;
    }

}
