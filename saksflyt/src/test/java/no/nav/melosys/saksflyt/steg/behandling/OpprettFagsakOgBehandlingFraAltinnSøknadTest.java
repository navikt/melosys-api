package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpprettFagsakOgBehandlingFraAltinnSøknadTest {

    @Mock
    private AltinnSoeknadService altinnSoeknadService;

    private OpprettFagsakOgBehandlingFraAltinnSøknad opprettFagsakOgBehandlingFraAltinnSøknad;

    private final String soeknadID = "abc123";
    private final Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
    private final Prosessinstans prosessinstans = Prosessinstans.builder().medType(ProsessType.OPPRETT_SAK).medStatus(ProsessStatus.KLAR).build();

    @BeforeEach
    public void setup() {
        opprettFagsakOgBehandlingFraAltinnSøknad = new OpprettFagsakOgBehandlingFraAltinnSøknad(altinnSoeknadService);

        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, soeknadID);
        when(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soeknadID))
            .thenReturn(behandling);
    }

    @Test
    public void utfør_behandlingBlirOpprettetVerifiserNesteSteg() {
        opprettFagsakOgBehandlingFraAltinnSøknad.utfør(prosessinstans);
        verify(altinnSoeknadService).opprettFagsakOgBehandlingFraAltinnSøknad(eq(soeknadID));
        assertThat(prosessinstans.getBehandling()).isEqualTo(behandling);
    }
}
