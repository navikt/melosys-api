package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.medlemskap.Periodetype;
import no.nav.melosys.service.kodeverk.KodeDto;

public class MedlemsperiodeDto {

    public PeriodeDto periode;

    public Periodetype medlemtype;

    public KodeDto status;

    public KodeDto grunnlagstype;

    public KodeDto land;

    public KodeDto lovvalg;

    public KodeDto trygdedekning;

    public KodeDto kildedokumenttype;

    public KodeDto kilde;
}
