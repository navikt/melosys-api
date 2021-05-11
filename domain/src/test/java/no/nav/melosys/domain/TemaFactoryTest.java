package no.nav.melosys.domain;

import no.nav.melosys.exception.FunksjonellException;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER;
import static org.assertj.core.api.Assertions.assertThat;

public class TemaFactoryTest {

    @Test
    public void fraBehandlingstema_tilMED() throws FunksjonellException {
        assertThat(TemaFactory.fraBehandlingstema(UTSENDT_ARBEIDSTAKER))
            .isEqualTo(Tema.MED);
    }

    @Test
    public void fraBehandlingstema_tilUFM() throws FunksjonellException {
        assertThat(TemaFactory.fraBehandlingstema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING))
            .isEqualTo(Tema.UFM);
    }
}
