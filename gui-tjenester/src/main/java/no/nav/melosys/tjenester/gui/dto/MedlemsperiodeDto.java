package no.nav.melosys.tjenester.gui.dto;

public class MedlemsperiodeDto {

    private PeriodeDto periode;

    private String type;

    private String status;

    private String grunnlagstype;

    private String land;

    private String trygdedekning;

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(String grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(String trygdedekning) {
        this.trygdedekning = trygdedekning;
    }
}
