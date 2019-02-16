package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
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

    private static final String SAKSBEHANDLER = "Z990007";

    private Fagsak fagsak;

    FagsakTjeneste instans;

    @Before
    public void setUp() throws Exception {
        SpringSubjectHandler.set(new TestSubjectHandler());
        fagsak = fagsakMedBehandlinger(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.AVSLUTTET);

        instans = lagFagsakTjeneste(fagsak);
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) throws Exception {
        Tilgang tilgang = mock(Tilgang.class);
        FagsakService fagsakService = mock(FagsakService.class);
        OppgaveService oppgaveService = mock(OppgaveService.class);
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        Oppgave oppgave = new Oppgave();
        oppgave.setTilordnetRessurs(SAKSBEHANDLER);
        when(oppgaveService.hentOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
        return new FagsakTjeneste(fagsakService, oppgaveService, tilgang);
    }

    @Test
    public final void aktivBehandlingFinnes_BehandlingDtoForAktivBehandlingErSkriveBar() throws Exception {

        Response resultat = instans.hentFagsak("123");

        assertThat(resultat.getStatusInfo()).isEqualTo(Status.OK);
        FagsakDto fagsakDto = (FagsakDto)resultat.getEntity();
        assertThat(fagsakDto.getBehandlinger().size()).isEqualTo(3);
        List<BehandlingDto> fagsakOppsummeringDtos = fagsakDto.getBehandlinger();
        assertThat(fagsakOppsummeringDtos.get(0).isRedigerbart()).isEqualTo(true);
        assertThat(fagsakOppsummeringDtos.get(1).isRedigerbart()).isEqualTo(false);
    }

    @Test
    public final void testErBehandlingRedigerBar() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        Oppgave oppgave = new Oppgave();
        oppgave.setTilordnetRessurs(SAKSBEHANDLER);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.of(oppgave), behandling)).isEqualTo(true);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.of(oppgave), null)).isEqualTo(false);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.empty(), null)).isEqualTo(false);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.empty(), behandling)).isEqualTo(false);
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.of(oppgave), behandling)).isEqualTo(false);
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(instans.erBehandlingRedigerbar(SAKSBEHANDLER, Optional.of(oppgave), behandling)).isEqualTo(false);
        assertThat(instans.erBehandlingRedigerbar("", Optional.of(oppgave), behandling)).isEqualTo(false);
    }
}