package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode {

    // TODO: Konverter datofelter via settere på joda format.
    //Periode periode;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate fom;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate tom;

    private Periodetype type;

    private String status;

    private String grunnlagstype;

    private String land;

    private String lovvalg;

    // TODO: "Lovvalg periode type" avventer avklaring

    private String trygdedekning;

    private String kildedokumenttype;

    private String kilde;

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public Periodetype getType() {
        return type;
    }

    public void setType(Periodetype type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(String grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getLovvalg() {
        return lovvalg;
    }

    public void setLovvalg(String lovvalg) {
        this.lovvalg = lovvalg;
    }

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(String trygdedekning) {
        this.trygdedekning = trygdedekning;
    }

    public String getKildedokumenttype() {
        return kildedokumenttype;
    }

    public void setKildedokumenttype(String kildedokumenttype) {
        this.kildedokumenttype = kildedokumenttype;
    }

    public String getKilde() {
        return kilde;
    }

    public void setKilde(String kilde) {
        this.kilde = kilde;
    }
}
