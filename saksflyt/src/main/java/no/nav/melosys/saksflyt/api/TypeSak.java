package no.nav.melosys.saksflyt.api;

public enum TypeSak {
    
    SOEKA1_V1("Søknad om A1 (versjon 1)");

    private String navn;
    
    private TypeSak(String navn) {
        this.navn = navn;
    }
    
    public String getNavn() {
        return navn;
    }
}
