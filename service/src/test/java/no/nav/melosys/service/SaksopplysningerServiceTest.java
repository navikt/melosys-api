package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaksopplysningerServiceTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private SaksopplysningerService saksopplysningerService;

    @Captor
    private ArgumentCaptor<Saksopplysning> captor;

    @BeforeEach
    void setUp() {
        saksopplysningerService = new SaksopplysningerService(saksopplysningRepository);
    }

    @Test
    void lagrePersonopplysninger_ok() {
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERSOPL);
    }

    @Test
    void lagrePersonMedHistorikk_ok() {
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERS_SAKS);
    }
}
