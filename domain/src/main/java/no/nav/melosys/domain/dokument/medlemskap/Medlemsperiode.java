package no.nav.melosys.domain.dokument.medlemskap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode implements HarPeriode {

    public Periode periode;

    public Periodetype type; //"http://nav.no/kodeverk/Kodeverk/PeriodetypeMedl"

    public String status; //"http://nav.no/kodeverk/Kodeverk/PeriodestatusMedl"

    public GrunnlagMedltype grunnlagstype; //"http://nav.no/kodeverk/Kodeverk/GrunnlagMedl"

    public String land; //"http://nav.no/kodeverk/Kodeverk/Landkoder"

    public String lovvalg; //"http://nav.no/kodeverk/Kodeverk/LovvalgMedl"

    public DekningMedltype trygdedekning; //"http://nav.no/kodeverk/Kodeverk/DekningMedl"

    public String kildedokumenttype; //"http://nav.no/kodeverk/Kodeverk/KildedokumentMedl"

    public String kilde; //"http://nav.no/kodeverk/Kodeverk/KildesystemMedl"

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    @Override
    public ErPeriode getPeriode() {
        return null;
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

    public GrunnlagMedltype getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(GrunnlagMedltype grunnlagstype) {
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

    public DekningMedltype getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(DekningMedltype trygdedekning) {
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
