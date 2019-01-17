
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Statsborgerskap {


    private String land;

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

}
