package no.nav.melosys.saksflytapi;

import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.saksflytapi.journalfoering.DokumentRequest;
import no.nav.melosys.saksflytapi.journalfoering.JournalfoeringOpprettRequest;
import no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest;
import no.nav.melosys.saksflytapi.journalfoering.Soeknadsland;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.assertj.core.api.Assertions;
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
    private ProsessinstansForServiceRepository prosessinstansRepo;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService prosessinstansService;

    @BeforeEach
    public void setUp() {
        prosessinstansService = new ProsessinstansService(applicationEventPublisher,
            prosessinstansRepo);
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


        prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling, resultatType, "FRITEKST", "FRITEKST_SED", Set.of(mottakerInstitusjon), true);


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
        assertThat(lagretInstans.getData(ProsessDataKey.BEGRUNNELSE_FRITEKST)).isNotBlank();
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
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new MottatteOpplysningerData());
        return behandling;
    }

    @Test
    void opprettProsessinstansOpprettOgDistribuerBrevBruker() {
        String saksbehandler = settInnloggetSaksbehandler();
        Behandling behandling = lagBehandling();
        Mottaker mottaker = new Mottaker();
        mottaker.setRolle(Mottakerroller.BRUKER);
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
        Mottaker mottaker = new Mottaker();
        mottaker.setRolle(Mottakerroller.ARBEIDSGIVER);
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
        Mottaker mottaker = new Mottaker();
        mottaker.setRolle(Mottakerroller.FULLMEKTIG);
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
        Mottaker mottaker = new Mottaker();
        mottaker.setRolle(Mottakerroller.FULLMEKTIG);
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
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();
        journalfoeringOpprettRequest.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        journalfoeringOpprettRequest.setAvsenderID("DK");
        final String institusjonsIdForDk = "ID_FOR_DK";


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringOpprettRequest, institusjonsIdForDk);


        assertThat(prosessinstans.getData(ProsessDataKey.AVSENDER_ID)).isEqualTo(institusjonsIdForDk);
    }

    @Test
    void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingFalse_settesIProsessinstans() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();

        journalfoeringOpprettRequest.setIkkeSendForvaltingsmelding(false);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringOpprettRequest, null);

        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansJournalføring_ikkeSendForvaltningsmeldingTrue_settesIProsessinstans() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();

        journalfoeringOpprettRequest.setIkkeSendForvaltingsmelding(true);


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringOpprettRequest, null);


        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansJournalføring_virksomhetOrgnr_settesIProsessinstans() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();

        journalfoeringOpprettRequest.setBrukerID(null);
        journalfoeringOpprettRequest.setVirksomhetOrgnr("orgnr");

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringOpprettRequest, null);

        assertThat(prosessinstans.getData(ProsessDataKey.VIRKSOMHET_ORGNR)).isEqualTo("orgnr");
    }


    @Test
    void opprettProsessinstansJournalføring_skalTilordnesTrue_settesIProsessinstans() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();

        journalfoeringOpprettRequest.setSkalTilordnes(true);


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringOpprettRequest, null);


        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansJournalføring_skalTilordnesFalse_settesIProsessinstans() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();
        journalfoeringOpprettRequest.setSkalTilordnes(false);


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.ANMODNING_OM_UNNTAK, journalfoeringOpprettRequest, null);


        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansJournalføring_medVedlegg_setterVedleggOgTitler() {
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();
        journalfoeringOpprettRequest.getHoveddokument().setDokumentID("hovedDokumentID");
        List<DokumentRequest> vedlegg = new ArrayList<>();
        DokumentRequest fysiskVedlegg = new DokumentRequest("dokID1", "tittel1", new ArrayList<>());
        vedlegg.add(fysiskVedlegg);
        DokumentRequest fysiskVedlegg2 = new DokumentRequest("hovedDokumentID", "Logisk ??", new ArrayList<>());
        vedlegg.add(fysiskVedlegg2);
        journalfoeringOpprettRequest.setVedlegg(vedlegg);
        List<String> logiskeVedlegg1 = journalfoeringOpprettRequest.getHoveddokument().getLogiskeVedlegg();
        logiskeVedlegg1.add("tittel");


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringOpprettRequest, null);


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
        JournalfoeringOpprettRequest journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest();
        journalfoeringOpprettRequest.getHoveddokument().setDokumentID("hovedDokumentID");
        List<DokumentRequest> vedlegg = new ArrayList<>();
        DokumentRequest fysiskVedlegg = new DokumentRequest("dokID1", "tittel1", Collections.emptyList());
        vedlegg.add(fysiskVedlegg);
        DokumentRequest fysiskVedlegg2 = new DokumentRequest("hovedDokumentID", "Logisk ??", Collections.emptyList());
        vedlegg.add(fysiskVedlegg2);
        journalfoeringOpprettRequest.setVedlegg(vedlegg);


        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK_BRUKER, journalfoeringOpprettRequest, null);


        var fysiskeVedleggTypeReference = new TypeReference<Map<String, String>>() {
        };
        assertThat(prosessinstans.getData(ProsessDataKey.FYSISKE_VEDLEGG, fysiskeVedleggTypeReference))
            .containsKeys(fysiskVedlegg.getDokumentID(), fysiskVedlegg2.getDokumentID())
            .containsValues(fysiskVedlegg.getTittel(), fysiskVedlegg2.getTittel());
    }

    @Test
    void opprettProsessinstansRegistrerUnntakFraMedlemskap_altOk_lagrerProsessinstans() {
        var behandling = lagBehandling();


        prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.AVSLUTTET);


        verify(prosessinstansRepo).save(piCaptor.capture());
        assertThat(piCaptor.getValue().getBehandling()).isEqualTo(behandling);
        assertThat(piCaptor.getValue().getData(ProsessDataKey.SAKSSTATUS, Saksstatuser.class)).isEqualTo(Saksstatuser.AVSLUTTET);
    }

    @Test
    void opprettProsessinstansGodkjennUnntaksperiodeMedEessiMelding() {
        MelosysEessiMelding melosysEessiMelding = lagMelosysEessiMelding();
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(new Behandling(), false, "fritekst", melosysEessiMelding);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_GODKJENN);
        assertThat(prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("fritekst");
        assertThat(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class)).isEqualTo(melosysEessiMelding);
        assertThat(prosessinstans.getLåsReferanse()).isEqualTo(melosysEessiMelding.lagUnikIdentifikator());
    }

    @Test
    void opprettProsessinstansGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(new Behandling(), false, "fritekst", null);


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_GODKJENN);
        assertThat(prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED)).isEqualTo("fritekst");
        assertThat(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class)).isNull();
    }

    @Test
    void opprettProsessinstansIkkeGodkjennUnntaksperiode() {
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(new Behandling(), "fritekst");


        verify(prosessinstansRepo).save(piCaptor.capture());

        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.REGISTRERING_UNNTAK_AVVIS);
        assertThat(prosessinstans.getData(ProsessDataKey.BEGRUNNELSE_FRITEKST)).isEqualTo("fritekst");
    }

    @Test
    void opprettProsessinstansNySak_behandlingstypeFørstegangTemaIkkeYrkesaktiv() {
        OpprettSakRequest opprettSakRequest = new EasyRandom().nextObject(OpprettSakRequest.class);
        opprettSakRequest.setSakstype(Sakstyper.EU_EOS);
        opprettSakRequest.setBehandlingstema(Behandlingstema.IKKE_YRKESAKTIV);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakRequest);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakRequest.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakRequest.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakRequest.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakRequest.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        Assertions.assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, no.nav.melosys.saksflytapi.journalfoering.Periode.class)).isEqualTo(opprettSakRequest.getSoknad().getPeriode());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, Soeknadsland.class)).isEqualTo(opprettSakRequest.getSoknad().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakRequest.getSkalTilordnes());
    }

    @Test
    void opprettProsessinstansNySak_behandlingstypeFørstegangTemaTrygdetid() {
        OpprettSakRequest opprettSakRequest = new EasyRandom().nextObject(OpprettSakRequest.class);
        opprettSakRequest.setSakstype(Sakstyper.EU_EOS);
        opprettSakRequest.setBehandlingstema(Behandlingstema.TRYGDETID);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakEØS(journalpostID, opprettSakRequest);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakRequest.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakRequest.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.TRYGDETID);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakRequest.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakRequest.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        Assertions.assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, no.nav.melosys.saksflytapi.journalfoering.Periode.class)).isEqualTo(opprettSakRequest.getSoknad().getPeriode());
        Assertions.assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND, Soeknadsland.class)).isEqualTo(opprettSakRequest.getSoknad().getLand());
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakRequest.getSkalTilordnes());
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
    void opprettProsessinstansNySakFTRL_altOk_setterProsessInstansKorrekt() {
        OpprettSakRequest opprettSakRequest = new EasyRandom().nextObject(OpprettSakRequest.class);
        opprettSakRequest.setSakstype(Sakstyper.FTRL);
        opprettSakRequest.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakRequest.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakRequest.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(journalpostID, opprettSakRequest);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.FTRL);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(Behandlingstyper.FØRSTEGANG);
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(Behandlingstema.YRKESAKTIV);
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakRequest.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakRequest.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakRequest.getSkalTilordnes());
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE)).isNull();
        assertThat(prosessinstans.getData(ProsessDataKey.SØKNADSLAND)).isNull();
    }

    @Test
    void opprettProsessinstansNySakFTRLTrygdeavtale_altOk_setterProsessInstansKorrekt() {
        OpprettSakRequest opprettSakRequest = new EasyRandom().nextObject(OpprettSakRequest.class);
        opprettSakRequest.setSakstype(Sakstyper.FTRL);
        opprettSakRequest.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        String journalpostID = "journalpostID";


        prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(journalpostID, opprettSakRequest);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE);
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(opprettSakRequest.getSakstype());
        assertThat(prosessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(opprettSakRequest.getSakstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class)).isEqualTo(opprettSakRequest.getBehandlingstype());
        assertThat(prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class)).isEqualTo(opprettSakRequest.getBehandlingstema());
        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(opprettSakRequest.getBrukerID());
        assertThat(prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)).isEqualTo(opprettSakRequest.getOppgaveID());
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo(journalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).isEqualTo(opprettSakRequest.getSkalTilordnes());
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
        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", false, false);

        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SOKNAD_ALTINN);
        assertThat(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID)).isEqualTo("søknadID");
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isTrue();
    }

    @Test
    void opprettProsessinstansSøknadMottatt_finnesFraFør_oppretterIkkeProsessinstans() {
        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", true, false);

        verify(prosessinstansRepo, never()).save(any(Prosessinstans.class));
    }

    @Test
    void opprettProsessinstansSøknadMottatt_mottakEldreEnnNoenDager_ikkeSendForvaltningsmelding() {
        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", false, true);


        verify(prosessinstansRepo).save(piCaptor.capture());
        Prosessinstans prosessinstans = piCaptor.getValue();
        assertThat(prosessinstans.getData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class)).isFalse();
    }

    @Test
    void opprettProsessinstansSendBrev_oppretterNyProsessinstans() {
        var behandling = new Behandling();
        var doksysbrevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build();
        var mottaker = Mottaker.medRolle(Mottakerroller.BRUKER);

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
        var mottakere = List.of(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER), Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET));


        prosessinstansService.opprettProsessinstanserSendBrev(behandling, doksysbrevbestilling, mottakere);


        verify(prosessinstansRepo, times(3)).save(piCaptor.capture());
        assertThat(piCaptor.getAllValues().get(0).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.medRolle(Mottakerroller.BRUKER)));
        assertThat(piCaptor.getAllValues().get(1).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)));
        assertThat(piCaptor.getAllValues().get(2).getData(ProsessDataKey.BREVBESTILLING, DoksysBrevbestilling.class).getMottakere())
            .isEqualTo(List.of(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)));
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

    private static JournalfoeringOpprettRequest lagJournalfoeringOpprettRequest() {
        JournalfoeringOpprettRequest journalfoeringRequest = new JournalfoeringOpprettRequest();
        journalfoeringRequest.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());
        journalfoeringRequest.setJournalpostID("journalpostid");
        journalfoeringRequest.setOppgaveID("oppgaveid");
        journalfoeringRequest.setBrukerID("brukerid");
        journalfoeringRequest.setAvsenderID("avsenderid");
        journalfoeringRequest.setAvsenderNavn("avsendernavn");
        journalfoeringRequest.setHoveddokument(new DokumentRequest("dokumentid", "hovedkokumenttittel", new ArrayList<>()));
        return journalfoeringRequest;
    }


    private String settInnloggetSaksbehandler() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);
        return saksbehandler;
    }
}
