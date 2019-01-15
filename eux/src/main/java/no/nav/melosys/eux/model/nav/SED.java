
package no.nav.melosys.eux.model.nav;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;

@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class SED {

    private Medlemskap medlemskap;

    private Nav nav;

    private String sed;

    private String sedGVer;

    private String sedVer;

    public Medlemskap getMedlemskap() {
        return medlemskap;
    }

    public void setMedlemskap(Medlemskap medlemskap) {
        this.medlemskap = medlemskap;
    }

    public Nav getNav() {
        return nav;
    }

    public void setNav(Nav nav) {
        this.nav = nav;
    }

    public String getSed() {
        return sed;
    }

    public void setSed(String sed) {
        this.sed = sed;
    }

    public String getSedGVer() {
        return sedGVer;
    }

    public void setSedGVer(String sedGVer) {
        this.sedGVer = sedGVer;
    }

    public String getSedVer() {
        return sedVer;
    }

    public void setSedVer(String sedVer) {
        this.sedVer = sedVer;
    }

}
