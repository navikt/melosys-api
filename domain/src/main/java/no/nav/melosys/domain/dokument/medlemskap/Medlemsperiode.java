package no.nav.melosys.domain.dokument.medlemskap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode {

    private Periode periode;

    private Periodetype type; //"http://nav.no/kodeverk/Kodeverk/PeriodetypeMedl"

    private String status; //"http://nav.no/kodeverk/Kodeverk/PeriodestatusMedl"

    private String grunnlagstype; //"http://nav.no/kodeverk/Kodeverk/GrunnlagMedl"

    private String land; //"http://nav.no/kodeverk/Kodeverk/Landkoder"

    private String lovvalg; //"http://nav.no/kodeverk/Kodeverk/LovvalgMedl"

    private String trygdedekning; //"http://nav.no/kodeverk/Kodeverk/DekningMedl"

    private String kildedokumenttype;

    private String kilde;

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
