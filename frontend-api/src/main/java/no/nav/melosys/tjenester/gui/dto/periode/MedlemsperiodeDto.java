package no.nav.melosys.tjenester.gui.dto.periode;

import no.nav.melosys.service.kodeverk.KodeDto;

public class MedlemsperiodeDto {

    private long periodeID;

    private PeriodeDto periode;

    private KodeDto periodetype;

    private KodeDto status;

    private KodeDto grunnlagstype;

    private KodeDto land;

    private KodeDto lovvalg;

    private KodeDto trygdedekning;

    private KodeDto kildedokumenttype;

    private KodeDto kilde;

    public long getPeriodeID() {
        return periodeID;
    }

    public void setPeriodeID(long periodeID) {
        this.periodeID = periodeID;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public KodeDto getPeriodetype() {
        return periodetype;
    }

    public void setPeriodetype(KodeDto periodetype) {
        this.periodetype = periodetype;
    }

    public KodeDto getStatus() {
        return status;
    }

    public void setStatus(KodeDto status) {
        this.status = status;
    }

    public KodeDto getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(KodeDto grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public KodeDto getLand() {
        return land;
    }

    public void setLand(KodeDto land) {
        this.land = land;
    }

    public KodeDto getLovvalg() {
        return lovvalg;
    }

    public void setLovvalg(KodeDto lovvalg) {
        this.lovvalg = lovvalg;
    }

    public KodeDto getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(KodeDto trygdedekning) {
        this.trygdedekning = trygdedekning;
    }

    public KodeDto getKildedokumenttype() {
        return kildedokumenttype;
    }

    public void setKildedokumenttype(KodeDto kildedokumenttype) {
        this.kildedokumenttype = kildedokumenttype;
    }

    public KodeDto getKilde() {
        return kilde;
    }

    public void setKilde(KodeDto kilde) {
        this.kilde = kilde;
    }
}
