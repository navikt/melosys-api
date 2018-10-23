package no.nav.melosys.domain.dokument.medlemskap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import no.nav.melosys.domain.HarPeriode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode implements HarPeriode {

    private Periode periode;

    private Periodetype type; //"http://nav.no/kodeverk/Kodeverk/PeriodetypeMedl"

    private String status; //"http://nav.no/kodeverk/Kodeverk/PeriodestatusMedl"

    private GrunnlagMedl grunnlagstype; //"http://nav.no/kodeverk/Kodeverk/GrunnlagMedl"

    private String land; //"http://nav.no/kodeverk/Kodeverk/Landkoder"

    private String lovvalg; //"http://nav.no/kodeverk/Kodeverk/LovvalgMedl"

    private DekningMedl trygdedekning; //"http://nav.no/kodeverk/Kodeverk/DekningMedl"

    private String kildedokumenttype; //"http://nav.no/kodeverk/Kodeverk/KildedokumentMedl"

    private String kilde; //"http://nav.no/kodeverk/Kodeverk/KildesystemMedl"

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    @Override
    public Periode getPeriode() {
        return periode;
    }

    public void setType(Periodetype type) {
        this.type = type;
    }

    public Periodetype getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public GrunnlagMedl getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(GrunnlagMedl grunnlagstype) {
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

    public DekningMedl getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(DekningMedl trygdedekning) {
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
