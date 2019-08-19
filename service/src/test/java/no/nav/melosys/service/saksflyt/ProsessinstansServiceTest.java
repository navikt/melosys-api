package no.nav.melosys.service.saksflyt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
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
        when(prosessinstansRepo.findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(eq(ProsessType.OPPFRISKNING), anyLong(), eq(ProsessSteg.FEILET_MASKINELT), eq(ProsessSteg.FERDIG)))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(service.erUnderOppfriskning(1L)).isTrue();
    }

    @Test
    public void harAktivProsessinstans() {
        when(prosessinstansRepo.findByBehandling_IdAndStegIsNotAndStegIsNot(anyLong(), eq(ProsessSteg.FEILET_MASKINELT), eq(ProsessSteg.FERDIG)))
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
    public void opprettProsessinstansForkortPeriode() {
        String saksbehandler = settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        service.opprettProsessinstansForkortPeriode(behandling, Endretperiode.RETURNERT_NORGE);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.IV_FORKORT_PERIODE);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
        assertThat(lagretInstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class)).isEqualTo(Endretperiode.RETURNERT_NORGE);
    }

    @Test
    public void opprettProsessinstansJournalføring_skalTilordnesTrue_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringDto journalfoeringDto = lagJournalfoeringDTO();

        journalfoeringDto.setSkalTilordnes(true);

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isTrue();
    }

    @Test
    public void opprettProsessinstansJournalføring_skalTilordnesFalse_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringDto journalfoeringDto = lagJournalfoeringDTO();

        journalfoeringDto.setSkalTilordnes(false);

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isFalse();
    }

    @Test
    public void opprettProsessinstansJournalføring_medVedlegg_setterVedleggOgTitler() {
        settInnloggetSaksbehandler();
        JournalfoeringDto journalfoeringDto = lagJournalfoeringDTO();
        journalfoeringDto.setDokumentID("hovedDokumentID");
        List<DokumentDto> vedlegg = new ArrayList<>();
        DokumentDto fysiskVedlegg = new DokumentDto("ID_F_01", "Fysisk");
        vedlegg.add(fysiskVedlegg);
        DokumentDto logiskVedlegg_1 = new DokumentDto(null, "Logisk");
        vedlegg.add(logiskVedlegg_1);
        DokumentDto logiskVedlegg_2 = new DokumentDto("hovedDokumentID", "Logisk ??");
        vedlegg.add(logiskVedlegg_2);
        journalfoeringDto.setVedlegg(vedlegg);

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, List.class)).contains(logiskVedlegg_1.getTittel());
        assertThat(prosessinstans.getData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, List.class)).contains(logiskVedlegg_2.getTittel());

        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, Map.class)).containsOnlyKeys(fysiskVedlegg.getDokumentID());
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, Map.class)).containsValues(fysiskVedlegg.getTittel());
    }

    @Test
    public void opprettProsessinstansGodkjennUnntaksperiode() {
        service.opprettProsessinstansGodkjennUnntaksperiode(new Behandling());
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
    }

    @Test
    public void opprettProsessinstansIkkeGodkjennUnntaksperiode() {
        service.opprettProsessinstansUnntaksperiodeAvvist(new Behandling(),
            Lists.newArrayList(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND), "fritekst");
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, List.class))
            .contains(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isEqualTo("fritekst");
    }

    private static JournalfoeringDto lagJournalfoeringDTO() {
        JournalfoeringDto journalfoeringDto = new JournalfoeringDto();
        journalfoeringDto.setJournalpostID("journalpostid");
        journalfoeringDto.setDokumentID("dokumentid");
        journalfoeringDto.setOppgaveID("oppgaveid");
        journalfoeringDto.setBrukerID("brukerid");
        journalfoeringDto.setAvsenderID("avsenderid");
        journalfoeringDto.setAvsenderNavn("avsendernavn");
        journalfoeringDto.setHoveddokumentTittel("hovedkokumenttittel");
        return journalfoeringDto;
    }

    private String settInnloggetSaksbehandler() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        return saksbehandler;
    }
}
