package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.TekniskException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RegisteropplysningerRequestTest {

    @Test
    void valider_ingenBehandlingID_forventException() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> RegisteropplysningerRequest.builder().build())
            .withMessageContaining("BehandlingID er påkrevd for å hente registeropplysninger");
    }

    @Test
    void valider_ingenSaksopplysningstype_forventException() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> RegisteropplysningerRequest.builder().behandlingID(1L).build())
            .withMessageContaining("Krever minst én saksopplysningstype for å hente registeropplysninger");
    }

    @Test
    void valider_ingenFnrMenPåkrevd_forventException() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> RegisteropplysningerRequest.builder()
                .behandlingID(1L)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .build())
                .build())
            .withMessageContaining("Krever at fnr er satt ved henting av ")
            .withMessageContaining(SaksopplysningType.MEDL.getBeskrivelse())
            .withMessageContaining(SaksopplysningType.PERSOPL.getBeskrivelse());
    }

    @Test
    void valider_ingenFnrMenIkkePåkrevd_forventFnrLikNull() {
        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                .organisasjonsopplysninger()
                .build())
            .build();

        assertThat(registeropplysningerRequest.getFnr()).isNull();
    }
}
