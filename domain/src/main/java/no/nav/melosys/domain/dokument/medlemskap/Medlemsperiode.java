package no.nav.melosys.domain.dokument.medlemskap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.util.IsoLandkodeKonverterer;

@XmlAccessorType(XmlAccessType.FIELD)
public class Medlemsperiode implements HarPeriode {

    private static final String KILDE_LÅNEKASSEN = "LAANEKASSEN";

    public Long id;

    public Periode periode;

    public String type; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodetypeMedl

    public String status; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodestatusMedl

    public String grunnlagstype; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/GrunnlagMedl

    public String land; // ISO3, https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Landkoder

    public String lovvalg; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/LovvalgMedl

    public String trygdedekning; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/DekningMedl

    public String kildedokumenttype; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildedokumentMedl

    public String kilde; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildesystemMedl

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

    public boolean erKildeLånekassen() {
        return KILDE_LÅNEKASSEN.equals(kilde);
    }

    public String hentLandSomIso2() {
        return land != null ? IsoLandkodeKonverterer.tilIso2(land) : null;
    }
}
