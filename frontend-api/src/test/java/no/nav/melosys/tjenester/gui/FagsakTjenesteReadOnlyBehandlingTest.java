package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.tjenester.gui.util.FagsakBehandlingFactory.fagsakMedBehandlinger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteReadOnlyBehandlingTest {

    private static final String FNR = "12345678901";

    @Before
    public void setUp() {
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) throws Exception {
        Tilgang tilgang = mock(Tilgang.class);
        FagsakService fagsakService = mock(FagsakService.class);
        OppgaveService oppgaveService = mock(OppgaveService.class);
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        Oppgave oppgave = new Oppgave();
        oppgave.setTilordnetRessurs("Z990007");
        when(oppgaveService.hentOppgaveMedFagSaksnummer(fagsak.getSaksnummer())).thenReturn(oppgave);
        ArrayList<Fagsak> fagsaker = new ArrayList<>();
        fagsaker.add(fagsak);
        doReturn(fagsaker).when(fagsakService).hentFagsakerMedAktør(eq(Aktoersroller.BRUKER), eq(FNR));
        return new FagsakTjeneste(fagsakService, oppgaveService, tilgang);
    }

    @Test
    public final void finnesAktivBehandling_BehandlingDtoForAktivBehandlingErSkriveBart() throws Exception {
        Fagsak fagsak = fagsakMedBehandlinger(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.AVSLUTTET);

        FagsakTjeneste instans = lagFagsakTjeneste(fagsak);
        Response resultat = instans.hentFagsak("123");

        assertThat(resultat.getStatusInfo()).isEqualTo(Status.OK);
        FagsakDto fagsakDto = (FagsakDto)resultat.getEntity();
        assertThat(fagsakDto.getBehandlinger().size()).isEqualTo(3);
        List<BehandlingDto> fagsakOppsummeringDtos = fagsakDto.getBehandlinger();
        assertThat(fagsakOppsummeringDtos.get(0).isRedigerbart()).isEqualTo(true);
        assertThat(fagsakOppsummeringDtos.get(1).isRedigerbart()).isEqualTo(false);
    }

}