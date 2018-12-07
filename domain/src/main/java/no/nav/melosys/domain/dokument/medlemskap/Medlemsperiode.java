package no.nav.melosys.domain.dokument.medlemskap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import no.nav.melosys.domain.HarPeriode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode implements HarPeriode {

    public Long id;

    public Periode periode;

    public String type; //"http://nav.no/kodeverk/Kodeverk/PeriodetypeMedl"

    public String status; //"http://nav.no/kodeverk/Kodeverk/PeriodestatusMedl"

    public String grunnlagstype; //"http://nav.no/kodeverk/Kodeverk/GrunnlagMedl"

    public String land; //"http://nav.no/kodeverk/Kodeverk/Landkoder"

    public String lovvalg; //"http://nav.no/kodeverk/Kodeverk/LovvalgMedl"

    public String trygdedekning; //"http://nav.no/kodeverk/Kodeverk/DekningMedl"

    public String kildedokumenttype; //"http://nav.no/kodeverk/Kodeverk/KildedokumentMedl"

    public String kilde; //"http://nav.no/kodeverk/Kodeverk/KildesystemMedl"

    @Override
    public Periode getPeriode() {
        return periode;
    }
    
    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getGrunnlagstype() {
        return grunnlagstype;
    }

    public String getLand() {
        return land;
    }

    public String getLovvalg() {
        return lovvalg;
    }

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public String getKildedokumenttype() {
        return kildedokumenttype;
    }

    public String getKilde() {
        return kilde;
    }

}
