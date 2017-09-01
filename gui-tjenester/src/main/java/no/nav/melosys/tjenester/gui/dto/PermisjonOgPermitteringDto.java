
package no.nav.melosys.tjenester.gui.dto;

public class PermisjonOgPermitteringDto {

    private String permisjonsId;

    private PeriodeDto permisjonsPeriode;

    private Float permisjonsprosent;

    private String permisjonOgPermittering;

    public PermisjonOgPermitteringDto() {
    }

    /**
     *
     * @param permisjonsPeriode
     * @param permisjonsId
     */
    public PermisjonOgPermitteringDto(String permisjonsId, PeriodeDto permisjonsPeriode) {
        super();
        this.permisjonsId = permisjonsId;
        this.permisjonsPeriode = permisjonsPeriode;
    }

    public String getPermisjonsId() {
        return permisjonsId;
    }

    public void setPermisjonsId(String permisjonsId) {
        this.permisjonsId = permisjonsId;
    }

    public PeriodeDto getPermisjonsPeriode() {
        return permisjonsPeriode;
    }

    public void setPermisjonsPeriode(PeriodeDto permisjonsPeriode) {
        this.permisjonsPeriode = permisjonsPeriode;
    }

    public Float getPermisjonsprosent() {
        return permisjonsprosent;
    }

    public void setPermisjonsprosent(Float permisjonsprosent) {
        this.permisjonsprosent = permisjonsprosent;
    }

    public String getPermisjonOgPermittering() {
        return permisjonOgPermittering;
    }
    
    public void setPermisjonOgPermittering(String permisjonOgPermittering) {
        this.permisjonOgPermittering = permisjonOgPermittering;
    }

}