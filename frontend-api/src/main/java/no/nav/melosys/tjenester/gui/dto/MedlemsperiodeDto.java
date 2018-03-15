package no.nav.melosys.tjenester.gui.dto;

import java.util.Map;

import no.nav.melosys.domain.dokument.medlemskap.Periodetype;

public class MedlemsperiodeDto {

    public PeriodeDto periode;

    public Periodetype type;

    public Map<String, String> status;

    public Map<String, String> grunnlagstype;

    public Map<String, String> land;

    public Map<String, String> lovvalg;

    public Map<String, String> trygdedekning;

    public Map<String, String> kildedokumenttype;

    public Map<String, String> kilde;
}
