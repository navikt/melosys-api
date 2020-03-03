package no.nav.melosys.service.registeropplysninger;

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
    public void valider_ingenBehandlingID_forventException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("BehandlingID er påkrevd for å hente registeropplysninger");
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
}