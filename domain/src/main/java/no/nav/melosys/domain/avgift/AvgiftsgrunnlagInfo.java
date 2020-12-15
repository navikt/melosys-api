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

    public boolean getErSkattepliktig() {
        return erSkattepliktig;
    }

    public boolean getBetalerArbeidsgiverAvgift() {
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
}