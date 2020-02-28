package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.TekniskException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisteropplysningerRequestTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void valider_ingenBehandlingEllerBehandlingID_forventException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Behandling eller behandlingID er påkrevd for å hente registeropplysninger");
        RegisteropplysningerRequest.builder().build();
    }

    @Test
    public void valider_ingenSaksopplysningstype_forventException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Krever minst én saksopplysningstype for å hente registeropplysninger");
        RegisteropplysningerRequest.builder().behandlingID(1L).build();
    }

    @Test
    public void valider_ingenFnrMenPåkrevd_forventException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Krever at fnr er satt ved henting av ");
        expectedException.expectMessage(SaksopplysningType.MEDL.getBeskrivelse());
        expectedException.expectMessage(SaksopplysningType.PERSOPL.getBeskrivelse());

        RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .personopplysninger()
                .medlemskapsopplysninger()
                .organisasjonsopplysninger()
                .build())
            .build();
    }

    @Test
    public void valider_ingenFnrMenIkkePåkrevd_forventFnrLikNull() throws TekniskException {
        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .organisasjonsopplysninger()
                .build())
            .build();

        assertThat(registeropplysningerRequest.getFnr()).isNull();
    }

    @Test
    public void valider_feilIPeriode_forventException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Feil i periode:");
        expectedException.expectMessage(SaksopplysningType.INNTK.getBeskrivelse());
        expectedException.expectMessage(SaksopplysningType.ARBFORH.getBeskrivelse());
        expectedException.expectMessage("krever en gyldig periode");

        RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .fnr("123")
            .fom(null)
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .arbeidsforholdopplysninger()
                .inntektsopplysninger()
                .organisasjonsopplysninger()
                .build())
            .build();
    }

    @Test
    public void valider_feilIPeriodeMenIkkePåkrevd_forventPeriodeLikNull() throws TekniskException {
        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .fnr("123")
            .fom(null)
            .tom(null)
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .personopplysninger()
                .organisasjonsopplysninger()
                .build())
            .build();

        assertThat(registeropplysningerRequest.getFom()).isNull();
        assertThat(registeropplysningerRequest.getTom()).isNull();
    }

    @Test
    public void valider_bådeBehandlingOgBehandlingID_forventBehandling() throws TekniskException {
        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .behandling(new Behandling())
            .fnr("123")
            .fom(LocalDate.now())
            .tom(LocalDate.now().plusDays(1))
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder().personopplysninger().build())
            .build();

        assertThat(registeropplysningerRequest.getBehandlingID()).isNull();
        assertThat(registeropplysningerRequest.getBehandling()).isNotNull();
    }
}