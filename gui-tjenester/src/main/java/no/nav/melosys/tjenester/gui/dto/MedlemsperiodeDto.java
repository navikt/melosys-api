package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.tjenester.gui.dto.util.DtoUtils;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;

public class MedlemsperiodeDto {

    private PeriodeDto periode;

    private String type;

    private String status;

    private String grunnlagstype;

    private String land;

    private String trygdedekning;

    public static MedlemsperiodeDto toDto(Medlemsperiode m) {
        MedlemsperiodeDto medlemsperiodeDto = new MedlemsperiodeDto();

        LocalDate fom = DtoUtils.tilLocalDate(m.getFraOgMed());
        LocalDate tom = DtoUtils.tilLocalDate(m.getTilOgMed());
        medlemsperiodeDto.setPeriode(new PeriodeDto(fom, tom));

        medlemsperiodeDto.setType(m.getType().getValue());
        medlemsperiodeDto.setStatus(m.getStatus().getValue());
        medlemsperiodeDto.setGrunnlagstype(m.getGrunnlagstype().getValue());
        if (m.getLand() != null)
            medlemsperiodeDto.setLand(m.getLand().getValue());
        if (m.getTrygdedekning() != null)
            medlemsperiodeDto.setTrygdedekning(m.getTrygdedekning().getValue());

        return medlemsperiodeDto;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(String trygdedekning) {
        this.trygdedekning = trygdedekning;
    }
}
