
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Vedtak {


    private String artikkelforordning;

    private String datoforrigevedtak;

    private String erendringsvedtak;

    private GjelderPeriode gjelderperiode;

    private String gjeldervarighetyrkesaktivitet;

    private String land;

    private String eropprinneligvedtak;

    public String getArtikkelforordning() {
        return artikkelforordning;
    }

    public void setArtikkelforordning(String artikkelforordning) {
        this.artikkelforordning = artikkelforordning;
    }

    public String getDatoforrigevedtak() {
        return datoforrigevedtak;
    }

    public void setDatoforrigevedtak(String datoforrigevedtak) {
        this.datoforrigevedtak = datoforrigevedtak;
    }

    public String getErendringsvedtak() {
        return erendringsvedtak;
    }

    public void setErendringsvedtak(String erendringsvedtak) {
        this.erendringsvedtak = erendringsvedtak;
    }

    public GjelderPeriode getGjelderperiode() {
        return gjelderperiode;
    }

    public void setGjelderperiode(GjelderPeriode gjelderperiode) {
        this.gjelderperiode = gjelderperiode;
    }

    public String getGjeldervarighetyrkesaktivitet() {
        return gjeldervarighetyrkesaktivitet;
    }

    public void setGjeldervarighetyrkesaktivitet(String gjeldervarighetyrkesaktivitet) {
        this.gjeldervarighetyrkesaktivitet = gjeldervarighetyrkesaktivitet;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getEropprinneligvedtak() {
        return eropprinneligvedtak;
    }

    public void setEropprinneligvedtak(String eropprinneligvedtak) {
        this.eropprinneligvedtak = eropprinneligvedtak;
    }
}
