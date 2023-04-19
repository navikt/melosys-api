package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper.*;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
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
        if (erSkattepliktig && !betalerArbeidsgiverAvgift && særligAvgiftsgruppe == MISJONÆR) {
            return false;
        }

        return !erSkattepliktig || !betalerArbeidsgiverAvgift;
    }

    public void validerLovligeKominasjonerLønnFraNorge() {
        if (særligAvgiftsgruppe == null && betalerArbeidsgiverAvgift) {
            return;
        }
        if (særligAvgiftsgruppe == ARBEIDSTAKER_MALAYSIA && betalerArbeidsgiverAvgift && !erSkattepliktig) {
            return;
        }
        if (særligAvgiftsgruppe == MISJONÆR && !betalerArbeidsgiverAvgift) {
            return;
        }
        throw new FunksjonellException("Ulovlig kombinasjon for lønn fra Norge: " + this);
    }

    public void validerLovligeKominasjonerLønnFraUtlandet() {
        if (særligAvgiftsgruppe == null && !betalerArbeidsgiverAvgift) {
            return;
        }
        if (særligAvgiftsgruppe == ARBEIDSTAKER_MALAYSIA && !betalerArbeidsgiverAvgift && !erSkattepliktig) {
            return;
        }
        if (særligAvgiftsgruppe == FN && !betalerArbeidsgiverAvgift && !erSkattepliktig) {
            return;
        }
        throw new FunksjonellException("Ulovlig kombinasjon for lønn fra utlandet: " + this);
    }

    @Override
    public String toString() {
        return "Særlig avgiftsgruppe = " + særligAvgiftsgruppe +
            ", betaler arbeidsgiver avgift = " + (betalerArbeidsgiverAvgift ? "ja": "nei") +
            ", skattepliktig = " + (erSkattepliktig ? "ja" : "nei");
    }
}
