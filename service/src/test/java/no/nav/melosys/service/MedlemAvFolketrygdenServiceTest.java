package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedlemAvFolketrygdenServiceTest {

    private final long behandlingsresultatID = 1291;

    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;

    @BeforeEach
    void init() {
        medlemAvFolketrygdenService = new MedlemAvFolketrygdenService(medlemAvFolketrygdenRepository);
    }

    @Test
    void hentBeregningsresultat_ikkeFunnet_kasterFeil() {
        when(medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .isInstanceOf(IkkeFunnetException.class)
            .hasMessage("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingsresultatID);
    }
}
