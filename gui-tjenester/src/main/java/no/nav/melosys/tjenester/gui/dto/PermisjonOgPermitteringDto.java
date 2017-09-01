
package no.nav.melosys.tjenester.gui.dto;

import java.math.BigDecimal;

public class PermisjonOgPermitteringDto {

    private String permisjonsId;

    private PeriodeDto permisjonsPeriode;

    private BigDecimal permisjonsprosent;

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

    public BigDecimal getPermisjonsprosent() {
        return permisjonsprosent;
    }

    public void setPermisjonsprosent(BigDecimal permisjonsprosent) {
        this.permisjonsprosent = permisjonsprosent;
    }

    public String getPermisjonOgPermittering() {
        return permisjonOgPermittering;
    }
    
    public void setPermisjonOgPermittering(String permisjonOgPermittering) {
        this.permisjonOgPermittering = permisjonOgPermittering;
    }

}