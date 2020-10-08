package no.nav.melosys.service.behandling;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.repository.BehandlingRepository;

@RunWith(MockitoJUnitRunner.class)
public class EndreBehandlingstemaServiceTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    private EndreBehandlingstemaService endreBehandlingstemaService;

    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;

    @Before
    public void setUp(){
        endreBehandlingstemaService = spy(new EndreBehandlingstemaService(behandlingRepository));
    }

    @Test
    public void hentMuligeBehandlingstemaForSoknad() {
        Behandling behandling = new Behandling();
        behandling.setTema(ARBEID_FLERE_LAND);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(behandling);
        assertThat(Arrays.asList(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_ETT_LAND_ØVRIG, IKKE_YRKESAKTIV, ARBEID_FLERE_LAND,
            ARBEID_NORGE_BOSATT_ANNET_LAND)).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstemaForSED() {
        Behandling behandling = new Behandling();
        behandling.setTema(ØVRIGE_SED_MED);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(behandling);
        assertThat(Arrays.asList(ØVRIGE_SED_MED, ØVRIGE_SED_UFM, TRYGDETID)).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstemaUgyldig() {
        Behandling behandling = new Behandling();
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(behandling);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void endreBehandlingstemaSoknad() {
        Behandling behandling = new Behandling();
        behandling.setId(11L);
        behandling.setTema(ARBEID_FLERE_LAND);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(behandling, UTSENDT_ARBEIDSTAKER);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(11L);
    }

    @Test
    public void endreBehandlingstemaSED() {
        Behandling behandling = new Behandling();
        behandling.setId(11L);
        behandling.setTema(TRYGDETID);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(behandling, ØVRIGE_SED_MED);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(11L);
    }

    @Test
    public void endreBehandlingstemaUgyldig() {
        Behandling behandling = new Behandling();
        behandling.setId(11L);
        behandling.setTema(ARBEID_FLERE_LAND);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(behandling, ØVRIGE_SED_MED);
        verify(behandlingRepository, never()).save(behandlingArgumentCaptor.capture());
    }
}
