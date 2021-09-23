package no.nav.melosys.service.vedtak;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.vedtak.data.FattetVedtakTestData;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private BehandlingsresultatService mockBehandlingsresultatService;

    @Mock
    private PersondataFasade mockPersondataFasade;

    @Captor
    private ArgumentCaptor<FattetVedtak> fattetVedtakCaptor;

    private FattetVedtakService fattetVedtakService;

    @BeforeEach
    void setUp() {
        final FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.pdl.vedtaksmelding");
        fattetVedtakService = new FattetVedtakService(mockFattetVedtakProducer, mockBehandlingService,
            mockBehandlingsresultatService, mockPersondataFasade, unleash);
    }

    @Test
    void fattetVedtakFtrl_skalPubliseres() {
        final long behandlingId = 123L;
        when(mockBehandlingService.hentBehandling(behandlingId)).thenReturn(FattetVedtakTestData.lagBehandling());
        when(mockBehandlingsresultatService.hentBehandlingsresultat(behandlingId)).thenReturn(FattetVedtakTestData.lagBehandlingsresultat());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        fattetVedtakService.publiserFattetVedtak(behandlingId);

        verify(mockFattetVedtakProducer).produserMelding(fattetVedtakCaptor.capture());

        FattetVedtak fattetVedtak = fattetVedtakCaptor.getValue();
        assertThat(fattetVedtak).isNotNull();
        assertThat(fattetVedtak.sak()).isNotNull();
        assertThat(fattetVedtak.lovvalgOgMedlemskapsperioder()).isNotNull();
    }
}
