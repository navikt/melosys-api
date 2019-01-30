
package no.nav.melosys.eux.model.nav;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import no.nav.melosys.eux.json.MedlemskapTypeResolver;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;

@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SED {

    private Medlemskap medlemskap;

    private Nav nav;

    private String sed;

    private String sedGVer;

    private String sedVer;

    public Medlemskap getMedlemskap() {
        return medlemskap;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "sed")
    @JsonTypeIdResolver(MedlemskapTypeResolver.class)
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
