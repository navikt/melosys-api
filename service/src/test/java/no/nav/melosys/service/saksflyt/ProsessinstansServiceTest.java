package no.nav.melosys.service.saksflyt;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProsessinstansServiceTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService service;

    @Before
    public void setUp() {
        service = new ProsessinstansService(prosessinstansRepo, applicationEventPublisher);
    }

    @Test
    public void erUnderOppfriskning() {
        when(prosessinstansRepo.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(eq(ProsessType.OPPFRISKNING), eq(ProsessSteg.FEILET_MASKINELT), anyLong()))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(service.erUnderOppfriskning(1L)).isTrue();
    }

    @Test
    public void harAktivProsessinstans() {
        when(prosessinstansRepo.findByStegIsNotNullAndStegIsNotAndBehandling_Id(eq(ProsessSteg.FEILET_MASKINELT), anyLong()))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(service.harAktivProsessinstans(1L)).isTrue();
    }

    @Test
    public void lagreProsessinstans_medSaksbehandler() {
        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        String saksbehandler = "Z123456";
        service.lagre(prosessinstans, saksbehandler);

        verify(prosessinstans).setEndretDato(any());
        verify(prosessinstans).setRegistrertDato(any());
        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    public void lagreProsessinstans_utenSaksbehandler_henterFraSubjectHandler() {
        String saksbehandler = settInnloggetSaksbehandler();

        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        service.lagre(prosessinstans);

        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    public void opprettProsessinstansAnmodningOmUnntak() {
        Behandling behandling = new Behandling();
        service.opprettProsessinstansAnmodningOmUnntak(behandling);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.AOU_VALIDERING);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void opprettProsessinstansIverksettVedtak_medBehandlingOgBehandlingsresultat() {
        Behandling behandling = new Behandling();
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        service.opprettProsessinstansIverksettVedtak(behandling, resultatType);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(Behandlingsresultattyper.valueOf(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);
    }

    @Test
    public void opprettProsessinstansHenleggeSak() {
        settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        service.opprettProsessinstansHenleggSak(behandling, Henleggelsesgrunner.ANNET, "");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.HENLEGG_SAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.HS_OPPDATER_RESULTAT);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void opprettProsessinstansOppfriskning() {
        Behandling behandling = new Behandling();
        String aktørID = "aktørID";
        String brukerID = "br";
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        service.opprettProsessinstansOppfriskning(behandling, aktørID, brukerID, soeknadDokument);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.OPPFRISKNING);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.JFR_HENT_PERS_OPPL);
    }

    @Test
    public void opprettProsessinstansOppdaterAvklarteFakta() {
        String saksbehandler = settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        service.opprettProsessinstansOppdaterAvklarteFakta(behandling, Endretperioder.RETURNERT_NORGE);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_ENDRET_PERIODE);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.OPPDATER_AVKLARTE_FAKTA_ENDRETPERIODE_BEGRUNNELSE);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
        assertThat(lagretInstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.class)).isEqualTo(Endretperioder.RETURNERT_NORGE);
    }

    private String settInnloggetSaksbehandler() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        return saksbehandler;
    }
}
