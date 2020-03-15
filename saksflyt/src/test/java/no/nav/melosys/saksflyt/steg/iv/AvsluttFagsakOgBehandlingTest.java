package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    private AvsluttFagsakOgBehandling agent;

    @Mock
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        agent = new AvsluttFagsakOgBehandling(fagsakService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-123";
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        behandling.setFagsak(fagsak);
        p.setBehandling(behandling);

        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        agent.utfør(p);

        verify(fagsakService).avsluttFagsakOgBehandling(eq(fagsak), eq(Saksstatuser.LOVVALG_AVKLART));
        assertThat(p.getSteg()).isEqualTo(IV_STATUS_BEH_AVSL);
    }
}