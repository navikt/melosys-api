package no.nav.melosys.service.saksopplysninger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.SaksopplysningerService;
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
class SaksoppplysningEventListenerTest {

    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private SaksoppplysningEventListener saksoppplysningEventListener;

    @BeforeEach
    void setUp() {
        saksoppplysningEventListener = new SaksoppplysningEventListener(saksopplysningerService, persondataFasade);
    }

    @Test
    void lagrePersonopplysninger_behandlingErIverksatt_personopplysningBlirLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);

        Personopplysninger personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger();
        when(persondataFasade.hentPerson(eq("aktørID"), eq(Informasjonsbehov.MED_FAMILIERELASJONER))).thenReturn(personopplysninger);
        PersonMedHistorikk personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk();
        when(persondataFasade.hentPersonMedHistorikk(eq("aktørID"))).thenReturn(personMedHistorikk);

        BehandlingEndretStatusEvent event = new BehandlingEndretStatusEvent(56L, Behandlingsstatus.IVERKSETTER_VEDTAK, behandling);
        saksoppplysningEventListener.lagrePersonopplysninger(event);

        verify(saksopplysningerService).lagrePersonopplysninger(behandling, personopplysninger);
        verify(saksopplysningerService).lagrePersonMedHistorikk(behandling, personMedHistorikk);
    }

    @Test
    void lagrePersonopplysning_behandlingEr_opplysningerBlirIkkeLagret() {
        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setStatus(Behandlingsstatus.TIDSFRIST_UTLOEPT);

        verifyNoInteractions(saksopplysningerService, persondataFasade);
    }
}
