package no.nav.melosys.service.saksflyt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
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

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProsessinstansServiceTest {
    private static final String AKTØR_ID = "aktørId";

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ProsessinstansRepository prosessinstansRepo;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private MottatteOpplysningerService mottatteOpplysningerService;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService prosessinstansService;
    private final FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    public void setUp() {
        prosessinstansService = new ProsessinstansService(applicationEventPublisher,
            prosessinstansRepo, utenlandskMyndighetService, mottatteOpplysningerService, unleash);
        unleash.enableAll();
    }

    @Test
    void harAktivProsessinstans() {
        when(prosessinstansRepo.findByBehandling_IdAndStatusIs(anyLong(), eq(ProsessStatus.KLAR)))
            .thenReturn(Optional.of(new Prosessinstans()));
        assertThat(prosessinstansService.harAktivProsessinstans(1L)).isTrue();
    }

    @Test
    void lagreProsessinstans_medSaksbehandler() {
        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        String saksbehandler = "Z123456";


        prosessinstansService.lagre(prosessinstans, saksbehandler, null);


        verify(prosessinstans).setStatus(ProsessStatus.KLAR);
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
        final Behandling behandling = new Behandling();
        final String mottakerInstitusjon = "SE:123";
        final DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");


        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, Set.of(mottakerInstitusjon),
            Set.of(dokumentReferanse), "FRITEKST_SED");


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {
        }).get(0))
            .isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getData(ProsessDataKey.VEDLEGG_SED, new TypeReference<Set<DokumentReferanse>>() {
        }))
            .isEqualTo(Set.of(dokumentReferanse));
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
    }

    @Test
    void opprettProsessinstansIverksettVedtak_medBehandlingOgBehandlingsresultat() {
        Behandling behandling = new Behandling();
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        String mottakerInstitusjon = "DE:2332";


        prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling, resultatType, "FRITEKST", "FRITEKST_SED", Set.of(mottakerInstitusjon));


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_EOS);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {
        }).get(0)).isEqualTo(mottakerInstitusjon);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(Behandlingsresultattyper.valueOf(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);
        assertThat(lagretInstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("FRITEKST_SED");
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
        DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");


        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling, null, "T", Set.of(dokumentReferanse));


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.VIDERESEND_SOKNAD);
        assertThat(lagretInstans.getData(ProsessDataKey.EESSI_MOTTAKERE, List.class)).isNull();
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST)).isNotBlank();
        assertThat(lagretInstans.getData(ProsessDataKey.VEDLEGG_SED, new TypeReference<Set<DokumentReferanse>>() {
        }))
            .isEqualTo(Set.of(dokumentReferanse));
    }

    private Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("12354");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(new MottatteOpplysningerData());
        return behandling;
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevBruker() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId("123");
        mottaker.setPersonIdent(null);
        mottaker.setOrgnr(null);

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .build();


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        assertThat(lagretInstans.getData(ProsessDataKey.MOTTAKER, String.class)).isEqualTo(mottaker.getRolle().name());
        assertThat(lagretInstans.getData(ProsessDataKey.AKTØR_ID)).isEqualTo(mottaker.getAktørId());
        MangelbrevBrevbestilling lagretBrevbestilling = (MangelbrevBrevbestilling) lagretInstans.getData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.class);
        assertThat(lagretBrevbestilling.getProduserbartdokument()).isEqualTo(MANGELBREV_BRUKER);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevArbeidsgiver() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.ARBEIDSGIVER);
        mottaker.setAktørId(null);
        mottaker.setPersonIdent(null);
        mottaker.setOrgnr("987654321");

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build();


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        assertThat(lagretInstans.getData(ProsessDataKey.MOTTAKER, String.class)).isEqualTo(mottaker.getRolle().name());
        assertThat(lagretInstans.getData(ProsessDataKey.ORGNR)).isEqualTo(mottaker.getOrgnr());
        MangelbrevBrevbestilling lagretBrevbestilling = (MangelbrevBrevbestilling) lagretInstans.getData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.class);
        assertThat(lagretBrevbestilling.getProduserbartdokument()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevRepresentantPerson() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setAktørId(null);
        mottaker.setPersonIdent("123");
        mottaker.setOrgnr(null);

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build();


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        assertThat(lagretInstans.getData(ProsessDataKey.MOTTAKER, String.class)).isEqualTo(mottaker.getRolle().name());
        assertThat(lagretInstans.getData(ProsessDataKey.PERSON_IDENT)).isEqualTo(mottaker.getPersonIdent());
        MangelbrevBrevbestilling lagretBrevbestilling = (MangelbrevBrevbestilling) lagretInstans.getData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.class);
        assertThat(lagretBrevbestilling.getProduserbartdokument()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevRepresentantOrganisasjon() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setAktørId(null);
        mottaker.setPersonIdent(null);
        mottaker.setOrgnr("987654321");

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build();


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        assertThat(lagretInstans.getData(ProsessDataKey.MOTTAKER, String.class)).isEqualTo(mottaker.getRolle().name());
        assertThat(lagretInstans.getData(ProsessDataKey.ORGNR)).isEqualTo(mottaker.getOrgnr());
        MangelbrevBrevbestilling lagretBrevbestilling = (MangelbrevBrevbestilling) lagretInstans.getData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.class);
        assertThat(lagretBrevbestilling.getProduserbartdokument()).isEqualTo(MANGELBREV_ARBEIDSGIVER);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansForkortPeriode() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();


        prosessinstansService.opprettProsessinstansForkortPeriode(behandling, null, null);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE);
        assertThat(lagretInstans.getData(ProsessDataKey.SAKSBEHANDLER)).isEqualTo(saksbehandler);
    }

    @Test
    void opprettProsessinstansJournalføring_utendlandskMyndighet_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();
        journalfoeringDto.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        journalfoeringDto.setAvsenderID("DK");
        final String institusjonsIdForDk = "ID_FOR_DK";
        when(utenlandskMyndighetService.finnInstitusjonID(journalfoeringDto.getAvsenderID())).thenReturn(Optional.of(institusjonsIdForDk));


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringDto);


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
    void opprettProsessinstansJournalføring_virksomhetOrgnr_settesIProsessinstans() {
        JournalfoeringOpprettDto journalfoeringDto = lagJournalfoeringOpprettDto();

        journalfoeringDto.setBrukerID(null);
        journalfoeringDto.setVirksomhetOrgnr("orgnr");

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringDto);

        assertThat(prosessinstans.getData(ProsessDataKey.VIRKSOMHET_ORGNR)).isEqualTo("orgnr");
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


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringDto);


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


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringDto);


        var fysiskeVedleggTypeReference = new TypeReference<Map<String, String>>() {
        };
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, fysiskeVedleggTypeReference))
            .containsKeys(fysiskVedlegg.getDokumentID(), fysiskVedlegg2.getDokumentID())
            .containsValues(fysiskVedlegg.getTittel(), fysiskVedlegg2.getTittel());
    }

    @Test
    void opprettProsessinstansGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(new Behandling(), false, "fritekst");


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_GODKJENN);
        assertThat(prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("fritekst");
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
    void opprettProsessinstansNySak_behandlingstypeSøknadTemaIkkeYrkesaktivToggleDisabled() {
        unleash.disableAll();
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.IKKE_YRKESAKTIV);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, SoeknadslandDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }

    @Test
    void opprettProsessinstansNySak_behandlingstypeSøknadTemaIkkeYrkesaktiv() {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.IKKE_YRKESAKTIV);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakDto.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakDto.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, SoeknadslandDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }

    @Test
    void opprettProsessinstansNySak_behandlingstypeSedTemaTrygdetidToggleDisabled() {
        unleash.disableAll();
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.TRYGDETID);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.SED);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.TRYGDETID);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, SoeknadslandDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }

    @Test
    void opprettProsessinstansNySak_behandlingstypeSedTemaTrygdetid() {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.TRYGDETID);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakDto.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakDto.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.TRYGDETID);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, PeriodeDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, SoeknadslandDto.class)).isEqualTo(opprettSakDto.getSoknadDto().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
    }


    @Test
    void opprettProsessinstansNySak_mottattAnmodningOmUnntak() {
        prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(lagMelosysEessiMelding(), AKTØR_ID);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.UNNTAK);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isEqualTo(AKTØR_ID);
    }

    @Test
    void opprettProsessinstansNySak_unntaksregistrering() {
        prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(lagMelosysEessiMelding(),
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            AKTØR_ID);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_NY_SAK);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.UNNTAK);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isEqualTo(AKTØR_ID);
    }

    @Test
    void opprettProsessinstansNySak_arbeidFlereLand() {
        prosessinstansService.opprettProsessinstansNySakArbeidFlereLand(lagMelosysEessiMelding(),
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.BESLUTNING_LOVVALG_NORGE, AKTØR_ID);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ARBEID_FLERE_LAND_NY_SAK);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(
            Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isEqualTo(AKTØR_ID);
    }

    @Test
    void opprettProsessinstansNySakFTRLTrygdeavtaleToggleDisabled_altOk_setterProsessInstansKorrekt() {
        unleash.disableAll();
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.FTRL);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.ARBEID_I_UTLANDET);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE)).isNull();
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND)).isNull();
    }

    @Test
    void opprettProsessinstansNySakFTRLTrygdeavtale_altOk_setterProsessInstansKorrekt() {
        OpprettSakDto opprettSakDto = new EasyRandom().nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(journalpostID, opprettSakDto);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(opprettSakDto.getSakstype());
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakDto.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakDto.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(opprettSakDto.getBehandlingstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakDto.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakDto.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakDto.isSkalTilordnes());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE)).isNull();
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND)).isNull();
    }

    @Test
    void behandleMottattMelding() {
        MelosysEessiMelding eessiMelding = lagMelosysEessiMelding();


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
    void opprettProsessinstansSøknadMottatt_finnesIkkeFraFør_oppretterProsessinstans() {
        SoknadMottatt søknadMottatt = new SoknadMottatt("søknadID", ZonedDateTime.now());


        prosessinstansService.opprettProsessinstansSøknadMottatt(søknadMottatt);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SOKNAD_ALTINN);
        assertThat(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID)).isEqualTo(søknadMottatt.getSoknadID());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansSøknadMottatt_finnesFraFør_oppretterIkkeProsessinstans() {
        SoknadMottatt søknadMottatt = new SoknadMottatt("søknadID", ZonedDateTime.now());
        when(mottatteOpplysningerService.harMottattSøknadMedEksternReferanseID(søknadMottatt.getSoknadID())).thenReturn(true);


        prosessinstansService.opprettProsessinstansSøknadMottatt(søknadMottatt);


        verify(prosessinstansRepo, never()).save(any(Prosessinstans.class));
    }

    @Test
    void opprettProsessinstansSøknadMottatt_mottakEldreEnnNoenDager_ikkeSendForvaltningsmelding() {
        SoknadMottatt søknadMottatt = new SoknadMottatt("søknadID", ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()));


        prosessinstansService.opprettProsessinstansSøknadMottatt(søknadMottatt);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansSendBrev_oppretterNyProsessinstans() {
        var behandling = new Behandling();
        var doksysbrevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build();
        var mottaker = Mottaker.av(Aktoersroller.BRUKER);


        prosessinstansService.opprettProsessinstansSendBrev(behandling, doksysbrevbestilling, mottaker);


        verify(prosessinstansRepo).save(piCaptor.capture());
        assertThat(piCaptor.getValue().getBehandling()).isEqualTo(behandling);
        assertThat(piCaptor.getValue().getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class))
            .extracting(Brevbestilling::getProduserbartdokument, DoksysBrevbestilling::getMottakere)
            .containsExactly(INNVILGELSE_YRKESAKTIV, List.of(mottaker));
    }

    @Test
    void opprettProsessinstanserSendBrev_flereMottakere_oppretterNyProsessinstansPerMottaker() {
        var behandling = new Behandling();
        var doksysbrevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build();
        var mottakere = List.of(Mottaker.av(Aktoersroller.BRUKER), Mottaker.av(Aktoersroller.ARBEIDSGIVER), Mottaker.av(Aktoersroller.TRYGDEMYNDIGHET));


        prosessinstansService.opprettProsessinstanserSendBrev(behandling, doksysbrevbestilling, mottakere);


        verify(prosessinstansRepo, times(3)).save(piCaptor.capture());
        assertThat(piCaptor.getAllValues().get(0).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.av(Aktoersroller.BRUKER)));
        assertThat(piCaptor.getAllValues().get(1).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.av(Aktoersroller.ARBEIDSGIVER)));
        assertThat(piCaptor.getAllValues().get(2).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.av(Aktoersroller.TRYGDEMYNDIGHET)));
    }

    private MelosysEessiMelding lagMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(LocalDate.EPOCH);
        periode.setTom(LocalDate.EPOCH.plusYears(1));
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
