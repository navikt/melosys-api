package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.math.BigDecimal;

import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;

public class PermisjonOgPermittering implements HarPeriode {

    private String permisjonsId;

    private Periode permisjonsPeriode;

    private BigDecimal permisjonsprosent;

    private String permisjonOgPermittering;
    
    @Override
    public Periode getPeriode() {
        return permisjonsPeriode;
    }

    public String getPermisjonsId() {
        return permisjonsId;
    }

    public void setPermisjonsId(String permisjonsId) {
        this.permisjonsId = permisjonsId;
    }

    public Periode getPermisjonsPeriode() {
        return permisjonsPeriode;
    }

    public void setPermisjonsPeriode(Periode permisjonsPeriode) {
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
