package no.nav.melosys.service;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedlemAvFolketrygdenServiceTest {

    private final long behandlingsresultatID = 1291;

    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Captor
    private ArgumentCaptor<MedlemAvFolketrygden> captor;

    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;

    @BeforeEach
    void init() {
        medlemAvFolketrygdenService = new MedlemAvFolketrygdenService(medlemAvFolketrygdenRepository, behandlingsresultatService);
    }

    @Test
    void hentBeregningsresultat_ikkeFunnet_kasterFeil() {
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .isInstanceOf(IkkeFunnetException.class)
            .hasMessage("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingsresultatID);
    }

    @Test
    void lagreBestemmelse_medlemAvFolketrygdenFinnesIkke_lagerNy() {
        var behandlingsresultat = new Behandlingsresultat();
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)).thenReturn(Optional.empty());
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);


        medlemAvFolketrygdenService.lagreBestemmelse(behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);


        verify(behandlingsresultatService).hentBehandlingsresultat(behandlingsresultatID);
        verify(medlemAvFolketrygdenRepository).save(captor.capture());
        assertThat(captor.getValue().getBestemmelse()).isEqualTo(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        assertThat(captor.getValue().getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    void lagreBestemmelse_medlemAvFolketrygdenFinnes_oppdatererEksisterende() {
        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E);
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)).thenReturn(Optional.of(medlemAvFolketrygden));


        medlemAvFolketrygdenService.lagreBestemmelse(behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);


        verifyNoInteractions(behandlingsresultatService);
        verify(medlemAvFolketrygdenRepository).save(captor.capture());
        assertThat(captor.getValue().getBestemmelse()).isEqualTo(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
    }
}
