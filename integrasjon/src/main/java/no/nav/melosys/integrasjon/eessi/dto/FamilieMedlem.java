package no.nav.melosys.integrasjon.eessi.dto;

public class FamilieMedlem {

    private String relasjon; //FAR ELLER MOR
    private String fornavn;
    private String etternavn;

    public String getRelasjon() {
        return relasjon;
    }

    public void setRelasjon(String relasjon) {
        this.relasjon = relasjon;
    }

    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }
}
