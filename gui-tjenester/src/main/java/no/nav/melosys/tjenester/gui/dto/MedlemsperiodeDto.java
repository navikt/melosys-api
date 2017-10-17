package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.tjenester.gui.dto.util.DtoUtils;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;

public class MedlemsperiodeDto {

    public static class ValueTerm<V> {
        private V value;
        private V term;

        ValueTerm(V value, V term) {
            this.value = value;
            this.term = term;
        }

        public V getValue() {
            return value;
        }

        public V getTerm() {
            return term;
        }
    }

    private PeriodeDto periode;

    private ValueTerm<String> type;

    private ValueTerm<String> status;

    private ValueTerm<String> grunnlagstype;

    private ValueTerm<String> land;

    private ValueTerm<String> trygdedekning;

    public static MedlemsperiodeDto toDto(Medlemsperiode m) {
        MedlemsperiodeDto medlemsperiodeDto = new MedlemsperiodeDto();

        LocalDate fom = DtoUtils.tilLocalDate(m.getFraOgMed());
        LocalDate tom = DtoUtils.tilLocalDate(m.getTilOgMed());
        medlemsperiodeDto.setPeriode(new PeriodeDto(fom, tom));

        medlemsperiodeDto.setType(new ValueTerm<>(m.getType().getValue(), m.getType().getTerm()));
        medlemsperiodeDto.setStatus(new ValueTerm<>(m.getStatus().getValue(), m.getStatus().getTerm()));
        medlemsperiodeDto.setGrunnlagstype(new ValueTerm<>(m.getGrunnlagstype().getValue(), m.getGrunnlagstype().getTerm()));
        if (m.getLand() != null)
            medlemsperiodeDto.setLand(new ValueTerm<>(m.getLand().getValue(), m.getLand().getTerm()));
        if (m.getTrygdedekning() != null)
            medlemsperiodeDto.setTrygdedekning(new ValueTerm<>(m.getTrygdedekning().getValue(), m.getTrygdedekning().getTerm()));

        return medlemsperiodeDto;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public ValueTerm<String> getType() {
        return type;
    }

    public void setType(ValueTerm<String> type) {
        this.type = type;
    }

    public ValueTerm<String> getStatus() {
        return status;
    }

    public void setStatus(ValueTerm<String> status) {
        this.status = status;
    }

    public ValueTerm<String> getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(ValueTerm<String> grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public ValueTerm<String> getLand() {
        return land;
    }

    public void setLand(ValueTerm<String> land) {
        this.land = land;
    }

    public ValueTerm<String> getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(ValueTerm<String> trygdedekning) {
        this.trygdedekning = trygdedekning;
    }
}
