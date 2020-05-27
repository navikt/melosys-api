package no.nav.melosys.service.saksflyt;

import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ProsessinstansRepository prosessinstansRepo;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService prosessinstansService;

    @Before
    public void setUp() {
        prosessinstansService = new ProsessinstansService(applicationEventPublisher, prosessinstansRepo, utenlandskMyndighetService);
    }

    @Test
    public void harAktivProsessinstans() {
        when(prosessinstansRepo.findByBehandling_IdAndStegIsNotAndStegIsNot(anyLong(), eq(ProsessSteg.FEILET_MASKINELT), eq(ProsessSteg.FERDIG)))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(prosessinstansService.harAktivProsessinstans(1L)).isTrue();
    }

    @Test
    public void lagreProsessinstans_medSaksbehandler() {
        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        String saksbehandler = "Z123456";
        prosessinstansService.lagre(prosessinstans, saksbehandler);

        verify(prosessinstans).setEndretDato(any());
        verify(prosessinstans).setRegistrertDato(any());
        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    public void lagreProsessinstans_utenSaksbehandler_henterFraSubjectHandler() {
        String saksbehandler = settInnloggetSaksbehandler();

        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        prosessinstansService.lagre(prosessinstans);

        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    public void opprettProsessinstansAnmodningOmUnntak() {
        final String mottakerInstitusjon = "SE:123";
        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, Set.of(mottakerInstitusjon), "FRITEKST_SED");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.AOU_VALIDERING);
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>(){}).get(0)).isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void opprettProsessinstansIverksettVedtak_medBehandlingOgBehandlingsresultat() {
        Behandling behandling = new Behandling();
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        String mottakerInstitusjon = "DE:2332";
        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, resultatType, "FRITEKST", "FRITEKST_SED", Set.of(mottakerInstitusjon), vedtakstype, "BEGRUNNELSE");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>(){}).get(0)).isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(Behandlingsresultattyper.valueOf(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);
        assertThat(lagretInstans.getData(ProsessDataKey.REVURDER_BEGRUNNELSE)).isEqualTo("BEGRUNNELSE");
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
        assertThat(Vedtakstyper.valueOf(lagretInstans.getData(ProsessDataKey.VEDTAKSTYPE))).isEqualTo(vedtakstype);
    }

    @Test
    public void opprettProsessinstansHenleggeSak() {
        settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansHenleggSak(behandling, Henleggelsesgrunner.ANNET, "");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.HENLEGG_SAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.HS_OPPDATER_RESULTAT);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void opprettProsessinstansVideresendSøknad() {
        settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling, null);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.VIDERESEND_SOKNAD);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.VS_OPPDATER_RESULTAT);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, List.class)).isNull();
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    private Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("12354");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        return behandling;
    }

    @Test
    public void opprettProsessinstansForkortPeriode() {
        String saksbehandler = settInnloggetSaksbehandler();

        Behandling behandling = lagBehandling();
        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, Endretperiode.RETURNERT_NORGE, null, null);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.IV_FORKORT_PERIODE);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
        assertThat(lagretInstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class)).isEqualTo(Endretperiode.RETURNERT_NORGE);
    }

    @Test
    public void opprettProsessinstansJournalføring_utendlandskMyndighet_settesIProsessinstans() throws TekniskException {
        settInnloggetSaksbehandler();
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        journalfoeringDto.setAvsenderID("DK");
        final String institusjonsIdForDk = "ID_FOR_DK";
        when(utenlandskMyndighetService.lagInstitusjonsId(Landkoder.DK)).thenReturn(institusjonsIdForDk);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.AVSENDER_ID)).isEqualTo(institusjonsIdForDk);
    }

    @Test
    public void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingFalse_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setIkkeSendForvaltingsmelding(false);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isTrue();
    }

    @Test
    public void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingTrue_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setIkkeSendForvaltingsmelding(true);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isFalse();
    }

    @Test
    public void opprettProsessinstansJournalføring_skalTilordnesTrue_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setSkalTilordnes(true);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isTrue();
    }

    @Test
    public void opprettProsessinstansJournalføring_skalTilordnesFalse_settesIProsessinstans() {
        settInnloggetSaksbehandler();
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setSkalTilordnes(false);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isFalse();
    }

    @Test
    public void opprettProsessinstansJournalføring_medVedlegg_setterVedleggOgTitler() {
        settInnloggetSaksbehandler();
        JournalfoeringDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.getHoveddokument().setDokumentID("hovedDokumentID");
        List<DokumentDto> vedlegg = new ArrayList<>();
        DokumentDto fysiskVedlegg = new DokumentDto("dokID1", "tittel1");
        vedlegg.add(fysiskVedlegg);
        DokumentDto fysiskVedlegg2 = new DokumentDto("hovedDokumentID", "Logisk ??");
        vedlegg.add(fysiskVedlegg2);
        journalfoeringDto.setVedlegg(vedlegg);
        journalfoeringDto.getHoveddokument().getLogiskeVedlegg().add("tittel");

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, Map.class)).containsKeys(fysiskVedlegg.getDokumentID(), fysiskVedlegg2.getDokumentID());
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, Map.class)).containsValues(fysiskVedlegg.getTittel(), fysiskVedlegg2.getTittel());
        List<String> logiskeVedlegg = prosessinstans.getData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, List.class);
        assertThat(logiskeVedlegg).containsExactly("tittel");
    }

    @Test
    public void opprettProsessinstansGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(new Behandling(), false);
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
    }

    @Test
    public void opprettProsessinstansIkkeGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(new Behandling(),
            Lists.newArrayList(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND), "fritekst");
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, List.class))
            .contains(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isEqualTo("fritekst");
    }

    @Test
    public void opprettProsessinstansGenerellSedBehandling() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.setBehandlingstemaKode(Behandlingstema.TRYGDETID.getKode());
        prosessinstansService.opprettProsessinstansGenerellSedBehandling(journalfoeringDto);

        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.SED_GENERELL_SAK);
        assertThat(prosessinstans.getData(ProsessDataKey.DOKUMENT_ID)).isEqualTo(journalfoeringDto.getHoveddokument().getDokumentID());
    }

    @Test
    public void opprettProsessinstansNySak_behandlingstypeIkkeStøttet_feiler() throws FunksjonellException {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        expectedException.expect(FunksjonellException.class);
        prosessinstansService.opprettProsessinstansNySak("journalpostID", opprettSakDto);
    }

    @Test
    public void opprettProsessinstansNySak_behandlingstypeSøknadTemaIkkeYrkesaktiv() throws FunksjonellException {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.IKKE_YRKESAKTIV);
        String journalpostID = "journalpostID";
        prosessinstansService.opprettProsessinstansNySak(journalpostID, opprettSakDto);
        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }

    @Test
    public void behandleMottattMelding() {
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(LocalDate.now(), LocalDate.now().plusYears(1));
        prosessinstansService.opprettProsessinstansSedMottak(eessiMelding);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans).isNotNull();
        assertThat(prosessinstans.getData()).isNotEmpty();

        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID)).isNotEmpty();
        assertThat(prosessinstans.getData(ProsessDataKey.EESSI_MELDING)).isNotEmpty();
    }

    private MelosysEessiMelding hentMelosysEessiMelding(LocalDate fom, LocalDate tom) {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }

    private static JournalfoeringOpprettDto lagJournalfoeringOpprettDto() {
        JournalfoeringOpprettDto journalfoeringOpprettDto = new JournalfoeringOpprettDto();
        journalfoeringOpprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());
        return lagJournalfoeringDto(journalfoeringOpprettDto);
    }

    private static JournalfoeringTilordneDto lagJournalfoeringTilordneDto() {
        JournalfoeringTilordneDto journalfoeringTilordneDto = new JournalfoeringTilordneDto();
        journalfoeringTilordneDto.setBehandlingstypeKode(Behandlingstyper.SOEKNAD.getKode());
        return lagJournalfoeringDto(journalfoeringTilordneDto);
    }

    private static <T extends JournalfoeringDto> T lagJournalfoeringDto(T journalfoeringDto) {
        journalfoeringDto.setJournalpostID("journalpostid");
        journalfoeringDto.setOppgaveID("oppgaveid");
        journalfoeringDto.setBrukerID("brukerid");
        journalfoeringDto.setAvsenderID("avsenderid");
        journalfoeringDto.setAvsenderNavn("avsendernavn");
        journalfoeringDto.setHoveddokument(new DokumentDto("dokumentid", "hovedkokumenttittel"));
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
