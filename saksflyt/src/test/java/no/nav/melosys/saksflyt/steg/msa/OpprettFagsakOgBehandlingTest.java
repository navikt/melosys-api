package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private AltinnSoeknadService altinnSoeknadService;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    private final String soeknadID = "abc123";
    private final Behandling behandling = new Behandling();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @Before
    public void setup() throws FunksjonellException, TekniskException {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(altinnSoeknadService);

        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, soeknadID);
        when(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soeknadID))
            .thenReturn(behandling);
    }

    @Test
    public void utfør_behandlingBlirOpprettetVerifiserNesteSteg() throws MelosysException {
        opprettFagsakOgBehandling.utfør(prosessinstans);
        verify(altinnSoeknadService).opprettFagsakOgBehandlingFraAltinnSøknad(eq(soeknadID));
        assertThat(prosessinstans.getBehandling()).isEqualTo(behandling);
    }
}