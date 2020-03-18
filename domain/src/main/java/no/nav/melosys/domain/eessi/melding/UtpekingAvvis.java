package no.nav.melosys.domain.eessi.melding;

public class UtpekingAvvis {

    private String begrunnelse;
    private Boolean etterspørInformasjon;
    private String nyttLovvalgsland;
    private String fritekst;

    public UtpekingAvvis() {
        // Brukes av Jackson
    }

    public UtpekingAvvis(String begrunnelse, boolean etterspørInformasjon, String nyttLovvalgsland, String fritekst) {
        this.begrunnelse = begrunnelse;
        this.etterspørInformasjon = etterspørInformasjon;
        this.nyttLovvalgsland = nyttLovvalgsland;
        this.fritekst = fritekst;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean isEtterspørInformasjon() {
        return etterspørInformasjon;
    }

    public void setEtterspørInformasjon(Boolean etterspørInformasjon) {
        this.etterspørInformasjon = etterspørInformasjon;
    }

    public String getNyttLovvalgsland() {
        return nyttLovvalgsland;
    }

    public void setNyttLovvalgsland(String nyttLovvalgsland) {
        this.nyttLovvalgsland = nyttLovvalgsland;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }
}
