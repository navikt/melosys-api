package no.nav.melosys.integrasjon.eessi.dto;

public class BucSedRelasjonDto {

    private String buc;
    private String forsteSed;
    private String fagomrade;

    public BucSedRelasjonDto() {
    }

    public String getBuc() {
        return buc;
    }

    public void setBuc(String buc) {
        this.buc = buc;
    }

    public String getForsteSed() {
        return forsteSed;
    }

    public void setForsteSed(String forsteSed) {
        this.forsteSed = forsteSed;
    }

    public String getFagomrade() {
        return fagomrade;
    }

    public void setFagomrade(String fagomrade) {
        this.fagomrade = fagomrade;
    }
}
