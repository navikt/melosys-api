package no.nav.melosys.service.aktoer;

public class AktoerDto {

    private String aktoerID;
    private String institusjonsID;
    private String orgnr;
    private String rolleKode;
    private String utenlandskPersonID;
    private String representererKode;

    public String getAktoerID() {
        return aktoerID;
    }

    public void setAktoerID(String aktoerID) {
        this.aktoerID = aktoerID;
    }

    public String getInstitusjonsID() {
        return institusjonsID;
    }

    public void setInstitusjonsID(String institusjonsID) {
        this.institusjonsID = institusjonsID;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getRolleKode() {
        return rolleKode;
    }

    public void setRolleKode(String rolleKode) {
        this.rolleKode = rolleKode;
    }

    public String getUtenlandskPersonID() {
        return utenlandskPersonID;
    }

    public void setUtenlandskPersonID(String utenlandskPersonID) {
        this.utenlandskPersonID = utenlandskPersonID;
    }

    public String getRepresentererKode() {
        return representererKode;
    }

    public void setRepresentererKode(String representererKode) {
        this.representererKode = representererKode;
    }
}
