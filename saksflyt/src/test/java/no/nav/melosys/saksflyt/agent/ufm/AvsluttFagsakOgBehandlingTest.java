package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandlingFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private OppdaterFagsakOgBehandlingFelles felles;

    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    @Before
    public void setup() {
        avsluttFagsakOgBehandling = new AvsluttFagsakOgBehandling(felles);
    }

    @Test
    public void utfør() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(felles).avsluttFagsakOgBehandling(eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART), eq(Behandlingsstatus.AVSLUTTET));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }

}