package no.nav.melosys.service.vedtak;

import java.time.Instant;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.vedtak.publisering.FattetVedtakProducer;
import no.nav.melosys.service.vedtak.publisering.FattetVedtakService;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FattetVedtakServiceTest {

    @Mock
    private FattetVedtakProducer mockFattetVedtakProducer;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private PersondataFasade mockPersondataFasade;

    @Captor
    private ArgumentCaptor<FattetVedtak> fattetVedtakCaptor;

    private FattetVedtakService fattetVedtakService;

    @BeforeEach
    void setUp() {
        fattetVedtakService = new FattetVedtakService(mockFattetVedtakProducer, mockBehandlingService, mockPersondataFasade);
    }

    @Test
    void fattetVedtakFtrl_skalPubliseres() {
        var behandlingId = 123L;
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(behandlingId)).thenReturn(lagBehandling(behandlingId));
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        fattetVedtakService.publiserFattetVedtak(behandlingId);


        verify(mockFattetVedtakProducer).produserMelding(fattetVedtakCaptor.capture());


        FattetVedtak fattetVedtak = fattetVedtakCaptor.getValue();
        assertThat(fattetVedtak).isNotNull();
        assertThat(fattetVedtak.sak()).isNotNull();
    }

    private Behandling lagBehandling(long behandlingId) {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.FTRL);
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setAktører(Set.of(SaksbehandlingDataFactory.lagBruker()));
        var behandling = new Behandling();
        behandling.setId(behandlingId);
        behandling.setFagsak(fagsak);
        return behandling;
    }
}
