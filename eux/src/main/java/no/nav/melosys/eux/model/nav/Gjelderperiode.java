
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Gjelderperiode {


    private Fastperiode fastperiode;

    public Fastperiode getFastperiode() {
        return fastperiode;
    }

    public void setFastperiode(Fastperiode fastperiode) {
        this.fastperiode = fastperiode;
    }

}
