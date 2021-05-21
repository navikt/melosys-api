package no.nav.melosys.service.avgift;

import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class AvgiftsgrunnlagInfoTest {

    @Test
    void validerLovligeKominasjonerLønnFraNorge_gyldigeKombinasjon_ingenFeil() {
        new AvgiftsgrunnlagInfo(true, true, null).
            validerLovligeKominasjonerLønnFraNorge();
        new AvgiftsgrunnlagInfo(true, false, MISJONÆR)
            .validerLovligeKominasjonerLønnFraNorge();
        new AvgiftsgrunnlagInfo(false, true, null)
            .validerLovligeKominasjonerLønnFraNorge();
        new AvgiftsgrunnlagInfo(false, true, ARBEIDSTAKER_MALAYSIA)
            .validerLovligeKominasjonerLønnFraNorge();
        new AvgiftsgrunnlagInfo(false, false, MISJONÆR)
            .validerLovligeKominasjonerLønnFraNorge();
    }

    @Test
    void validerLovligeKominasjonerLønnFraNorge_ugyldigeKombinasjoner_kasterFeil() {
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, true, FN);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, true, MISJONÆR);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, true, ARBEIDSTAKER_MALAYSIA);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, false, null);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, false, FN);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(true, false, ARBEIDSTAKER_MALAYSIA);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(false, true, FN);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(false, true, MISJONÆR);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(false, false, null);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(false, false, FN);
        assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(false, false, ARBEIDSTAKER_MALAYSIA);
    }

    @Test
    void validerLovligeKominasjonerLønnFraUtlandet_gyldigeKombinasjon_ingenFeil() {
        new AvgiftsgrunnlagInfo(true, false, null)
            .validerLovligeKominasjonerLønnFraUtlandet();
        new AvgiftsgrunnlagInfo(false, false, null)
            .validerLovligeKominasjonerLønnFraUtlandet();
        new AvgiftsgrunnlagInfo(false, false, FN)
            .validerLovligeKominasjonerLønnFraUtlandet();
        new AvgiftsgrunnlagInfo(false, false, ARBEIDSTAKER_MALAYSIA)
            .validerLovligeKominasjonerLønnFraUtlandet();
    }

    @Test
    void validerLovligeKominasjonerLønnFraUtlandet_ugyldigeKombinasjoner_kasterFeil() {
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, true, null);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, true, FN);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, true, MISJONÆR);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, true, ARBEIDSTAKER_MALAYSIA);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, false, FN);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, false, MISJONÆR);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(true, false, ARBEIDSTAKER_MALAYSIA);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(false, true, null);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(false, true, FN);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(false, true, MISJONÆR);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(false, true, ARBEIDSTAKER_MALAYSIA);
        assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(false, false, MISJONÆR);
    }

    private void assertThatValiderLovligeKombinasjonerLønnFraNorgeThrowsException(boolean erSkattepliktig, boolean betalerArbeidsgiverAvgift, Saerligeavgiftsgrupper særligAvgiftsgruppe) {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> new AvgiftsgrunnlagInfo(erSkattepliktig, betalerArbeidsgiverAvgift, særligAvgiftsgruppe)
                .validerLovligeKominasjonerLønnFraNorge())
            .withMessageContaining("Ulovlig kombinasjon for lønn fra Norge: ");
    }

    private void assertThatValiderLovligeKombinasjonerLønnFraUtlandetThrowsException(boolean erSkattepliktig, boolean betalerArbeidsgiverAvgift, Saerligeavgiftsgrupper særligAvgiftsgruppe) {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> new AvgiftsgrunnlagInfo(erSkattepliktig, betalerArbeidsgiverAvgift, særligAvgiftsgruppe)
                .validerLovligeKominasjonerLønnFraUtlandet())
            .withMessageContaining("Ulovlig kombinasjon for lønn fra utlandet: ");
    }
}
