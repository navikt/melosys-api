package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.util.IsoLandkodeKonverterer;

public class Medlemsperiode implements HarPeriode {

    private static final String KILDE_LÅNEKASSEN = "LAANEKASSEN";

    private Long id;

    private Periode periode;

    private String type; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodetypeMedl

    private String status; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PeriodestatusMedl

    private String grunnlagstype; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/GrunnlagMedl

    private String land; // ISO3, https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Landkoder

    private String lovvalg; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/LovvalgMedl

    private String trygdedekning; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/DekningMedl

    private String kildedokumenttype; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildedokumentMedl

    private String kilde; // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/KildesystemMedl

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGrunnlagstype(String grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public void setLovvalg(String lovvalg) {
        this.lovvalg = lovvalg;
    }

    public void setTrygdedekning(String trygdedekning) {
        this.trygdedekning = trygdedekning;
    }

    public void setKildedokumenttype(String kildedokumenttype) {
        this.kildedokumenttype = kildedokumenttype;
    }

    public void setKilde(String kilde) {
        this.kilde = kilde;
    }

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

    public boolean erUnntaksperiode() {
        return !"NOR".equals(land);
    }

    public boolean erMedlemskapsperiode() {
        return "NOR".equals(land);
    }
}
