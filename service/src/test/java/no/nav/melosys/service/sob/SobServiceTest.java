package no.nav.melosys.service.sob;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SobServiceTest {

    private static final String SAKSNUMMER = "MEL-1234";
    private static final long BEHANDING_ID = 1L;
    private static final String AKTØR_ID = "98987878";

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private BehandlingService behandlingService;

    @Captor
    private ArgumentCaptor<BehandlingStatusMapper> captor;

    private SobService sobService;

    @BeforeEach
    public void setUp() throws Exception {
        sobService = new SobService(sakOgBehandlingFasade, persondataFasade, behandlingService);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(lagBehandling());
    }

    @Test
    void sakOgBehandlingOpprettet_forventMapperMedVerdier() throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingOpprettet(BEHANDING_ID);

        verify(sakOgBehandlingFasade).sendBehandlingOpprettet(captor.capture());

        BehandlingStatusMapper behandlingStatusMapper = captor.getValue();
        assertThat(behandlingStatusMapper.getAktoerREF().getAktoerId()).isEqualTo(AKTØR_ID);
        assertThat(behandlingStatusMapper.getBehandlingsID()).contains(String.valueOf(BEHANDING_ID));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo(SAKSNUMMER);
        assertThat(behandlingStatusMapper.getSakstema().getValue()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    void sakOgBehandlingAvsluttet_forventMapperMedVerdier() throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingAvsluttet(BEHANDING_ID);

        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(captor.capture());

        BehandlingStatusMapper behandlingStatusMapper = captor.getValue();
        assertThat(behandlingStatusMapper.getAktoerREF().getAktoerId()).isEqualTo(AKTØR_ID);
        assertThat(behandlingStatusMapper.getBehandlingsID()).contains(String.valueOf(BEHANDING_ID));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo(SAKSNUMMER);
        assertThat(behandlingStatusMapper.getSakstema().getValue()).isEqualTo(Tema.UFM.getKode());
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setId(1L);

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(AKTØR_ID);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);

        return behandling;
    }
}
