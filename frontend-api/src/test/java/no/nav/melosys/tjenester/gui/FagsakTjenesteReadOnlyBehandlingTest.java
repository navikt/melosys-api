package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.tjenester.gui.util.FagsakBehandlingFactory.fagsakMedBehandlinger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FagsakTjenesteReadOnlyBehandlingTest {

    private static final String SAKSBEHANDLER = "Z990007";

    private Fagsak fagsak;

    FagsakTjeneste instans;

    @Mock
    FagsakService fagsakService;

    @Before
    public void setUp() {
        SpringSubjectHandler.set(new TestSubjectHandler());
        fagsak = fagsakMedBehandlinger(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.AVSLUTTET);

        instans = lagFagsakTjeneste(fagsak);
    }

    private FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) {
        Tilgang tilgang = mock(Tilgang.class);
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        return new FagsakTjeneste(fagsakService, tilgang);
    }

    @Test
    public final void aktivBehandlingFinnes_BehandlingDtoForAktivBehandlingErSkriveBar() throws Exception {
        when(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak)).thenReturn(Optional.of(fagsak.getBehandlinger().get(0)));
        Response resultat = instans.hentFagsak("123");
        assertThat(resultat.getStatusInfo()).isEqualTo(Status.OK);
        FagsakDto fagsakDto = (FagsakDto)resultat.getEntity();
        assertThat(fagsakDto.getBehandlinger().size()).isEqualTo(3);
        List<BehandlingDto> fagsakOppsummeringDtos = fagsakDto.getBehandlinger();
        assertThat(fagsakOppsummeringDtos.get(0).isRedigerbart()).isEqualTo(true);
        assertThat(fagsakOppsummeringDtos.get(1).isRedigerbart()).isEqualTo(false);
    }
}