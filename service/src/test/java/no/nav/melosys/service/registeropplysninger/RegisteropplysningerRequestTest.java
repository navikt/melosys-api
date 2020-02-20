package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisteropplysningerRequestTest {

    @Test(expected = TekniskException.class)
    public void valider_ingenBehandlingEllerBehandlingID_forventException() throws TekniskException {
        RegisteropplysningerRequest.builder().build();
    }

    @Test(expected = TekniskException.class)
    public void valider_ingenSaksopplysningstype_forventException() throws TekniskException {
        RegisteropplysningerRequest.builder().behandlingID(1L).build();
    }

    @Test
    public void valider_bådeBehandlingOgBehandlingID_forventBehandling() throws TekniskException {
        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(1L)
            .behandling(new Behandling())
            .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder().personopplysninger().build())
            .build();

        assertThat(registeropplysningerRequest.getBehandlingID()).isNull();
        assertThat(registeropplysningerRequest.getBehandling()).isNotNull();
    }
}