package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;

public class AvgiftsgrunnlagInfo {
    private final boolean erSkattepliktig;
    private final boolean betalerArbeidsgiverAvgift;
    private final Saerligeavgiftsgrupper særligAvgiftsgruppe;

    public AvgiftsgrunnlagInfo(boolean erSkattepliktig,
                               boolean betalerArbeidsgiverAvgift,
                               Saerligeavgiftsgrupper særligAvgiftsgruppe) {
        this.erSkattepliktig = erSkattepliktig;
        this.betalerArbeidsgiverAvgift = betalerArbeidsgiverAvgift;
        this.særligAvgiftsgruppe = særligAvgiftsgruppe;
    }

    public boolean erSkattepliktig() {
        return erSkattepliktig;
    }

    public boolean betalerArbeidsgiverAvgift() {
        return betalerArbeidsgiverAvgift;
    }

    public Saerligeavgiftsgrupper getSærligAvgiftsgruppe() {
        return særligAvgiftsgruppe;
    }

    public boolean erAvgiftspliktig() {
        if (erSkattepliktig && !betalerArbeidsgiverAvgift && særligAvgiftsgruppe == Saerligeavgiftsgrupper.MISJONÆR) {
            return false;
        }

        return !erSkattepliktig || !betalerArbeidsgiverAvgift;
    }

    @Override
    public String toString() {
        return "Særlig avgiftsgruppe = " + særligAvgiftsgruppe +
            ", betaler arbeidsgiver avgift = " + (betalerArbeidsgiverAvgift ? "ja": "nei") +
            ", skattepliktig = " + (erSkattepliktig ? "ja" : "nei");
    }
}
