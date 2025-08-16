package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void lagrePersonopplysninger_ingenSaksopplysninger_ok() {
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERSOPL);
    }

    @Test
    void lagrePersonMedHistorikk_ingenSaksopplysninger_ok() {
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERS_SAKS);
    }

    @Test
    void lagrePersonopplysninger_PERSOPLeksisterer_lagresPERSOPLfjernes() {
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        behandling.getSaksopplysninger().add(saksopplysning);

        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERSOPL);
        assertThat(behandling.getSaksopplysninger()).isEmpty();
    }

    @Test
    void lagrePersonMedHistorikk_PERSHISTeksisterer_lagresPERSHISTfjernes() {
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSHIST);
        behandling.getSaksopplysninger().add(saksopplysning);

        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);

        verify(saksopplysningRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(SaksopplysningType.PDL_PERS_SAKS);
        assertThat(behandling.getSaksopplysninger()).isEmpty();
    }

    @Test
    void hentPersonhistorikk_PDL_PERS_SAKSeksistererIkke_optionalEmpty() {
        when(saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERS_SAKS)).thenReturn(Optional.empty());

        Optional<PersonMedHistorikk> personMedHistorikk = saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L);

        assertThat(personMedHistorikk).isNotPresent();
    }

    @Test
    void hentPDLpersonopplysninger_eksistererFraDb_returneres() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(PersonopplysningerObjectFactory.lagPersonopplysninger());
        when(saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERSOPL))
            .thenReturn(Optional.of(saksopplysning));

        Persondata persondata = saksopplysningerService.hentPdlPersonopplysninger(1L);

        assertThat(persondata).isNotNull();
    }
}
