package no.nav.melosys.service.saksflyt;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.service.soknad.SoknadMottatt;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProsessinstansServiceTest {
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ProsessinstansRepository prosessinstansRepo;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService prosessinstansService;

    @BeforeEach
    public void setUp() {
        prosessinstansService = new ProsessinstansService(applicationEventPublisher,
            prosessinstansRepo, utenlandskMyndighetService, behandlingsgrunnlagService);
    }

    @Test
    public void harAktivProsessinstans() {
        when(prosessinstansRepo.findByBehandling_IdAndStatusIs(anyLong(), eq(ProsessStatus.KLAR)))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(prosessinstansService.harAktivProsessinstans(1L)).isTrue();
    }

    @Test
    void lagreProsessinstans_medSaksbehandler() {
        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        String saksbehandler = "Z123456";
        prosessinstansService.lagre(prosessinstans, saksbehandler);

        verify(prosessinstans).setStatus(eq(ProsessStatus.KLAR));
        verify(prosessinstans).setEndretDato(any());
        verify(prosessinstans).setRegistrertDato(any());
        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    void lagreProsessinstans_utenSaksbehandler_henterFraSubjectHandler() {
        String saksbehandler = settInnloggetSaksbehandler();

        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        prosessinstansService.lagre(prosessinstans);

        verify(prosessinstans).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        verify(applicationEventPublisher).publishEvent(any(ProsessinstansOpprettetEvent.class));
    }

    @Test
    void opprettProsessinstansAnmodningOmUnntak() {
        final String mottakerInstitusjon = "SE:123";
        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, Set.of(mottakerInstitusjon), "FRITEKST_SED");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {
        }).get(0)).isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    void opprettProsessinstansIverksettVedtak_medBehandlingOgBehandlingsresultat() {
        Behandling behandling = new Behandling();
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        String mottakerInstitusjon = "DE:2332";
        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        prosessinstansService.opprettProsessinstansIverksettVedtak(behandling, resultatType, "FRITEKST", "FRITEKST_SED", Set.of(mottakerInstitusjon), vedtakstype, "BEGRUNNELSE");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {
        }).get(0)).isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(Behandlingsresultattyper.valueOf(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);
        assertThat(lagretInstans.getData(ProsessDataKey.REVURDER_BEGRUNNELSE)).isEqualTo("BEGRUNNELSE");
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
        assertThat(Vedtakstyper.valueOf(lagretInstans.getData(ProsessDataKey.VEDTAKSTYPE))).isEqualTo(vedtakstype);
    }

    @Test
    void opprettProsessinstansFagsakHenlagt() {
        settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.HENLEGG_SAK);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    void opprettProsessinstansVideresendSøknad() {
        settInnloggetSaksbehandler();

        Behandling behandling = new Behandling();
        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling, null, "T");

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.VIDERESEND_SOKNAD);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, List.class)).isNull();
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isNotBlank();
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
    void opprettProsessinstansOpprettOgDistribuerBrevBruker() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId("123");
        mottaker.setOrgnr(null);

        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, behandling, mottaker);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertEquals(ProsessType.OPPRETT_OG_DISTRIBUER_BREV, lagretInstans.getType());
        assertEquals(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, lagretInstans.getData(ProsessDataKey.PRODUSERBART_BREV, Produserbaredokumenter.class));
        assertEquals(saksbehandler, lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER));
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevArbeidsgiver() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.ARBEIDSGIVER);
        mottaker.setAktørId(null);
        mottaker.setOrgnr("987654321");

        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, behandling, mottaker);

        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertEquals(ProsessType.OPPRETT_OG_DISTRIBUER_BREV, lagretInstans.getType());
        assertEquals(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, lagretInstans.getData(ProsessDataKey.PRODUSERBART_BREV, Produserbaredokumenter.class));
        assertEquals(saksbehandler, lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER));
    }

    @Test
    void opprettProsessinstansForkortPeriode() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();

        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, null, null);

        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansJournalføring_utendlandskMyndighet_settesIProsessinstans() throws TekniskException {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        journalfoeringDto.setAvsenderID("DK");
        final String institusjonsIdForDk = "ID_FOR_DK";
        when(utenlandskMyndighetService.lagInstitusjonsId(Landkoder.DK)).thenReturn(institusjonsIdForDk);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.AVSENDER_ID)).isEqualTo(institusjonsIdForDk);
    }

    @Test
    void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingFalse_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setIkkeSendForvaltingsmelding(false);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingTrue_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setIkkeSendForvaltingsmelding(true);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansJournalføring_skalTilordnesTrue_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setSkalTilordnes(true);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansJournalføring_skalTilordnesFalse_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setSkalTilordnes(false);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansJournalføring_medVedlegg_setterVedleggOgTitler() {
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

        var fysiskeVedleggTypeReference = new TypeReference<Map<String, String>>() {
        };
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, fysiskeVedleggTypeReference))
            .containsKeys(fysiskVedlegg.getDokumentID(), fysiskVedlegg2.getDokumentID())
            .containsValues(fysiskVedlegg.getTittel(), fysiskVedlegg2.getTittel());

        List<String> logiskeVedlegg = prosessinstans.getData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, new TypeReference<>() {
        });
        assertThat(logiskeVedlegg).containsExactly("tittel");
    }

    @Test
    void opprettProsessinstansJournalføring_medFysiskeVedlegg_setterVedleggOgTitler() {
        JournalfoeringDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.getHoveddokument().setDokumentID("hovedDokumentID");
        List<DokumentDto> vedlegg = new ArrayList<>();
        DokumentDto fysiskVedlegg = new DokumentDto("dokID1", "tittel1");
        vedlegg.add(fysiskVedlegg);
        DokumentDto fysiskVedlegg2 = new DokumentDto("hovedDokumentID", "Logisk ??");
        vedlegg.add(fysiskVedlegg2);
        journalfoeringDto.setVedlegg(vedlegg);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        var fysiskeVedleggTypeReference = new TypeReference<Map<String, String>>() {
        };
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, fysiskeVedleggTypeReference))
            .containsKeys(fysiskVedlegg.getDokumentID(), fysiskVedlegg2.getDokumentID())
            .containsValues(fysiskVedlegg.getTittel(), fysiskVedlegg2.getTittel());
    }

    @Test
    void opprettProsessinstansGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(new Behandling(), false);
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_GODKJENN);
    }

    @Test
    void opprettProsessinstansIkkeGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(new Behandling(),
            Lists.newArrayList(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND), "fritekst");
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_AVVIS);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, new TypeReference<List<String>>() {
        })).contains(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isEqualTo("fritekst");
    }

    @Test
    public void opprettProsessinstansNySak_behandlingstypeSøknadTemaIkkeYrkesaktiv() {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.IKKE_YRKESAKTIV);
        String journalpostID = "journalpostID";
        prosessinstansService.opprettProsessinstansNySak(journalpostID, opprettSakDto, Behandlingstyper.SOEKNAD);
        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK);
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
    void opprettProsessinstansNySak_behandlingstypeSedTemaTrygdetid() {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.TRYGDETID);
        String journalpostID = "journalpostID";
        prosessinstansService.opprettProsessinstansNySak(journalpostID, opprettSakDto, Behandlingstyper.SED);
        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.SED);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.TRYGDETID);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }

    @Test
    void behandleMottattMelding() {
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

    @Test
    void opprettProsessinstansSøknadMottatt_finnesIkkeFraFør_oppretterNyProsessinstans() {
        SoknadMottatt søknadMottatt = new SoknadMottatt("søknadID", ZonedDateTime.now());

        prosessinstansService.opprettProsessinstansSøknadMottatt(søknadMottatt);
        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SOKNAD_ALTINN);
        assertThat(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID)).isEqualTo(søknadMottatt.getSoknadID());
    }

    @Test
    void opprettProsessinstansSøknadMottatt_finnesFraFør_oppretterIkkeNyProsessinstans() {
        SoknadMottatt søknadMottatt = new SoknadMottatt("søknadID", ZonedDateTime.now());
        when(behandlingsgrunnlagService.harMottattSøknadMedEksternReferanseID(eq(søknadMottatt.getSoknadID()))).thenReturn(true);

        prosessinstansService.opprettProsessinstansSøknadMottatt(søknadMottatt);
        verify(prosessinstansRepo, never()).save(any(Prosessinstans.class));
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
