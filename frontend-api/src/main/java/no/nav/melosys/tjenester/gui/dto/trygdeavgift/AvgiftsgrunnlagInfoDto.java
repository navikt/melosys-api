package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;

public class AvgiftsgrunnlagInfoDto {
    private final boolean erSkattepliktig;
    private final boolean betalerArbeidsgiverAvgift;
    private final Saerligeavgiftsgrupper særligAvgiftsgruppe;

    public AvgiftsgrunnlagInfoDto(@JsonProperty("erSkattepliktig") boolean erSkattepliktig,
                                  @JsonProperty("betalerArbeidsgiverAvgift") boolean betalerArbeidsgiverAvgift,
                                  @JsonProperty("særligAvgiftsgruppe") Saerligeavgiftsgrupper særligAvgiftsgruppe) {
        this.erSkattepliktig = erSkattepliktig;
        this.betalerArbeidsgiverAvgift = betalerArbeidsgiverAvgift;
        this.særligAvgiftsgruppe = særligAvgiftsgruppe;
    }

    public boolean isErSkattepliktig() {
        return erSkattepliktig;
    }

    public boolean isBetalerArbeidsgiverAvgift() {
        return betalerArbeidsgiverAvgift;
    }

    public Saerligeavgiftsgrupper getSærligAvgiftsgruppe() {
        return særligAvgiftsgruppe;
    }

    public AvgiftsgrunnlagInfo til() {
        return new AvgiftsgrunnlagInfo(
            this.erSkattepliktig,
            this.betalerArbeidsgiverAvgift,
            this.særligAvgiftsgruppe
        );
    }

    public static AvgiftsgrunnlagInfoDto av(AvgiftsgrunnlagInfo avgiftsgrunnlagInfo) {
        return new AvgiftsgrunnlagInfoDto(
            avgiftsgrunnlagInfo.erSkattepliktig(),
            avgiftsgrunnlagInfo.betalerArbeidsgiverAvgift(),
            avgiftsgrunnlagInfo.getSærligAvgiftsgruppe()
        );
    }
}
