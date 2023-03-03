package no.nav.melosys.domain.eessi.sed;

public class InvalideringSedDto {
    private String sedTypeSomSkalInvalideres;
    private String utstedelsedato;

    public String getSedTypeSomSkalInvalideres() {
        return sedTypeSomSkalInvalideres;
    }

    public void setSedTypeSomSkalInvalideres(String sedTypeSomSkalInvalideres) {
        this.sedTypeSomSkalInvalideres = sedTypeSomSkalInvalideres;
    }

    public String getUtstedelsedato() {
        return utstedelsedato;
    }

    public void setUtstedelsedato(String utstedelsedato) {
        this.utstedelsedato = utstedelsedato;
    }
}
