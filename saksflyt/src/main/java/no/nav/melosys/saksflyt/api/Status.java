package no.nav.melosys.saksflyt.api;

public enum Status {
    
    SOEKA1_V1_NY(TypeSak.SOEKA1_V1, "Ny"),
    SOEKA1_V1_KLARGJORT(TypeSak.SOEKA1_V1, "Klargjort");

    private TypeSak type;
    private String navn;
    
    private Status(TypeSak type, String navn) {
        this.type = type;
        this.navn = navn;
    }

    public TypeSak getType() {
        return type;
    }

    public String getNavn() {
        return navn;
    }

}
