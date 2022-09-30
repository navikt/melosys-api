package no.nav.melosys.service.saksopplysninger;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaksopplysningEventListenerTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private SaksoppplysningEventListener saksoppplysningEventListener;

    @BeforeEach
    void setUp() {
        saksoppplysningEventListener = new SaksoppplysningEventListener(saksopplysningerService, behandlingService, persondataFasade,
            avklartefaktaService);
    }

    @Test
    void lagrePersonopplysninger_behandlingErIverksattMedFamilie_personopplysningMedFamileBlirLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(ingenMedfolgendeFamilie());
        when(avklartefaktaService.hentAvklarteMedfølgendeEktefelle(anyLong())).thenReturn(lagMedfolgendeFamilie());
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        when(persondataFasade.hentPerson("aktørID", Informasjonsbehov.MED_FAMILIERELASJONER)).thenReturn(personopplysninger);
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        when(persondataFasade.hentPersonMedHistorikk("aktørID")).thenReturn(personMedHistorikk);

        BehandlingEndretStatusEvent event = new BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling);
        saksoppplysningEventListener.lagrePersonopplysninger(event);

        verify(persondataFasade).hentPerson(anyString(), eq(Informasjonsbehov.MED_FAMILIERELASJONER));
        verify(saksopplysningerService).lagrePersonopplysninger(behandling, personopplysninger);
        verify(saksopplysningerService).lagrePersonMedHistorikk(behandling, personMedHistorikk);
    }

    @Test
    void lagrePersonopplysninger_behandlingErIverksattUtenFamilie_personopplysningUtenFamileBlirLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(ingenMedfolgendeFamilie());
        when(avklartefaktaService.hentAvklarteMedfølgendeEktefelle(anyLong())).thenReturn(ingenMedfolgendeFamilie());
        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        when(persondataFasade.hentPerson("aktørID")).thenReturn(personopplysninger);
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        when(persondataFasade.hentPersonMedHistorikk("aktørID")).thenReturn(personMedHistorikk);

        BehandlingEndretStatusEvent event = new BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling);
        saksoppplysningEventListener.lagrePersonopplysninger(event);

        verify(persondataFasade).hentPerson(anyString());
        verify(saksopplysningerService).lagrePersonopplysninger(behandling, personopplysninger);
        verify(saksopplysningerService).lagrePersonMedHistorikk(behandling, personMedHistorikk);
    }

    @Test
    void lagrePersonopplysning_behandlingHarIkkeRelevantStatus_opplysningerBlirIkkeLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.TIDSFRIST_UTLOEPT);
        BehandlingEndretStatusEvent event = new BehandlingEndretStatusEvent(Behandlingsstatus.TIDSFRIST_UTLOEPT, behandling);

        saksoppplysningEventListener.lagrePersonopplysninger(event);

        verifyNoInteractions(saksopplysningerService, persondataFasade);
    }

    @Test
    void lagrePersonopplysning_hovedpartErVirksomhet_opplysningerBlirIkkeLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        behandling.getFagsak().setAktører(Set.of(virksomhet));
        BehandlingEndretStatusEvent event = new BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling);

        saksoppplysningEventListener.lagrePersonopplysninger(event);

        verifyNoInteractions(saksopplysningerService, persondataFasade);
    }

    private AvklarteMedfolgendeFamilie ingenMedfolgendeFamilie() {
        return new AvklarteMedfolgendeFamilie(Collections.emptySet(), Collections.emptySet());
    }

    private AvklarteMedfolgendeFamilie lagMedfolgendeFamilie() {
        return new AvklarteMedfolgendeFamilie(Set.of(new OmfattetFamilie("adfa")), Collections.emptySet());
    }
}
