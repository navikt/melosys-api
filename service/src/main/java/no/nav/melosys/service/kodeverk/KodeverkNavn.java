package no.nav.melosys.service.kodeverk;

public enum KodeverkNavn {
    
    LANDKODER("Landkoder");
    
    KodeverkNavn(String navn) {
        this.navn = navn;
    }
    
    private String navn;
    
    String getNavn() {
        return navn;
    }

}
