
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Pin {

    public Pin() {}

    public Pin(String identifikator, String land, String sektor) {
        this.identifikator = identifikator;
        this.land = land;
        this.sektor = sektor;
    }

    private String identifikator;

    private String land;

    private String sektor;

    public String getIdentifikator() {
        return identifikator;
    }

    public void setIdentifikator(String identifikator) {
        this.identifikator = identifikator;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getSektor() {
        return sektor;
    }

    public void setSektor(String sektor) {
        this.sektor = sektor;
    }

}
