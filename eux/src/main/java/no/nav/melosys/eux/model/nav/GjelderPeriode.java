
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class GjelderPeriode {

    private AapenPeriode aapenperiode;

    private Fastperiode fastperiode;

    public AapenPeriode getAapenperiode() {
        return aapenperiode;
    }

    public void setAapenperiode(AapenPeriode aapenperiode) {
        this.aapenperiode = aapenperiode;
    }

    public Fastperiode getFastperiode() {
        return fastperiode;
    }

    public void setFastperiode(Fastperiode fastperiode) {
        this.fastperiode = fastperiode;
    }

}
