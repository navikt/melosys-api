package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerInnvilgelse;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.Test;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarInnvilgelse;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagBostedsadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

public final class DokumentServiceTest {

    private static final long BEHANDLINGSID = 13L;
    private static final long GSAKSNUMMER = 321L;
    private static final String ORGNR = "123456789";

    private static long idTeller = 1L;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final DoksysFasade dokSysFasade;
    private final DokumentService instans;
    private final ProsessinstansService prosessinstansService;
    private final BehandlingsresultatService behandlingsresultatService;

    public DokumentServiceTest() throws Exception {
        avklarteVirksomheterService = mock(AvklarteVirksomheterService.class);
        dokSysFasade = mock(DoksysFasade.class);
        prosessinstansService = mock(ProsessinstansService.class);
        behandlingsresultatService = mock(BehandlingsresultatService.class);
        instans = lagDokumentService(null);
    }

    @Test
    public final void produserInnvilgelsesbrev_medFullmektig_senderTilBrukerOgFullmektig() throws Exception {
        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock());
        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(INNVILGELSE_YRKESAKTIV).build();
        dokumentServiceMedMockVelger.produserDokument(INNVILGELSE_YRKESAKTIV, Mottaker.av(BRUKER), BEHANDLINGSID, brevbestilling);
        verify(dokSysFasade, times(2)).produserIkkeredigerbartDokument(any(Dokumentbestilling.class));
    }

    @Test
    public final void produser_avslagArbeidsgiver_funker() throws Exception {
        Brevbestilling brevbestilling = lagBrevbestillingAvslagArbeidsgiver();
        Set<String> arbeidsgivendeOrgnumre = Collections.singleton("987654321");
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any(Behandling.class))).thenReturn(arbeidsgivendeOrgnumre);
        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock());
        dokumentServiceMedMockVelger.produserDokument(AVSLAG_ARBEIDSGIVER, Mottaker.av(ARBEIDSGIVER), BEHANDLINGSID, brevbestilling);
        verify(dokSysFasade).produserIkkeredigerbartDokument(any(Dokumentbestilling.class));
    }

    @Test
    public final void produserUtkast_innvilgelsesBrev_funker() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(BRUKER);

        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling));
        byte[] resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, INNVILGELSE_YRKESAKTIV, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(Dokumentbestilling.class));
    }

    @Test
    public final void produserUtkast_avslagArbeidsgiver_funker() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(ARBEIDSGIVER);
        Set<String> arbeidsgivendeOrgnumre = Collections.singleton("987654321");
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any(Behandling.class))).thenReturn(arbeidsgivendeOrgnumre);

        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling));
        byte[] resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, AVSLAG_ARBEIDSGIVER, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(Dokumentbestilling.class));
    }

    @Test
    public final void produserMangelbrevISaksflyt() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(BRUKER);
        instans.produserDokumentISaksflyt(MELDING_MANGLENDE_OPPLYSNINGER, brevbestilling.mottaker, BEHANDLINGSID, new BrevData(brevbestilling));
    }

    @Test
    public final void produserForvaltningsmeldingISaksflyt_fungerer() throws MelosysException {
        instans.produserDokumentISaksflyt(MELDING_FORVENTET_SAKSBEHANDLINGSTID, BRUKER, BEHANDLINGSID, null);

        verify(prosessinstansService).opprettProsessinstansForvaltningsmelding(argThat(b -> BEHANDLINGSID == b.getId()));
    }

    private static BrevbestillingDto lagBrevBestillingDto(Aktoersroller rolle) {
        BrevbestillingDto brevbestilling = new BrevbestillingDto();
        brevbestilling.mottaker = rolle;
        return brevbestilling;
    }

    @Test
    public final void produserMangelbrevISaksflyt_utenMottaker_kasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(MELDING_MANGLENDE_OPPLYSNINGER, null, BEHANDLINGSID, null));
        assertThat(unntak).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflytUtenBehandlingKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(INNVILGELSE_YRKESAKTIV, BRUKER, ~BEHANDLINGSID, null));
        assertThat(unntak).isInstanceOfAny(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserUkjentDokumenttypeISaksflytKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(MELDING_HENLAGT_SAK, ARBEIDSGIVER, BEHANDLINGSID, null));
        assertThat(unntak).isInstanceOfAny(FunksjonellException.class).hasNoCause().hasMessageContaining("er ikke støttet");
    }

    @Test
    public final void produserDokumentUtenBehandlingKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(ATTEST_A1, Mottaker.av(ARBEIDSGIVER), ~BEHANDLINGSID, new Brevbestilling.Builder().build()));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserDokumentUtenDokumenttypeKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(null, Mottaker.av(ARBEIDSGIVER), BEHANDLINGSID, new Brevbestilling.Builder().build()));
        assertThat(unntak).isInstanceOf(IllegalArgumentException.class).hasNoCause().hasMessageContaining("Ingen gyldig");
    }

    private static BrevData lagBrevDataInnvilgelse() {
        BrevDataA1 brevDataA1 = new BrevDataA1();
        AvklartVirksomhet arbeidsgiver = new AvklartVirksomhet("Virker av og til", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevDataA1.hovedvirksomhet = arbeidsgiver;
        brevDataA1.bostedsadresse = lagStrukturertAdresse();
        brevDataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevDataA1.bivirksomheter = Collections.emptyList();
        brevDataA1.person = lagPersonDokument();
        brevDataA1.arbeidssteder = new ArrayList<>();
        BrevDataInnvilgelse brevdataInnvilgelse = new BrevDataInnvilgelse(new BrevbestillingDto(), "SAKSBEHANDLER");
        brevdataInnvilgelse.vedleggA1 = brevDataA1;
        brevdataInnvilgelse.hovedvirksomhet = arbeidsgiver;
        brevdataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevdataInnvilgelse.avklartMaritimType = Maritimtyper.SKIP;
        brevdataInnvilgelse.arbeidsland = "Norway";
        brevdataInnvilgelse.anmodningsperiodesvar = Optional.of(lagAnmodningsperiodeSvarInnvilgelse());
        brevdataInnvilgelse.trygdemyndighetsland = "Denmark";

        return brevdataInnvilgelse;
    }

    private static BrevData lagBrevDataAvslagArbeidsgiver() {
        BrevDataAvslagArbeidsgiver brevDataAvslagArbeidsgiver = new BrevDataAvslagArbeidsgiver("Z007");
        brevDataAvslagArbeidsgiver.person = lagPersonDokument();
        brevDataAvslagArbeidsgiver.hovedvirksomhet = new AvklartVirksomhet("Virker 100%", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevDataAvslagArbeidsgiver.lovvalgsperiode = lagLovvalgsperiode();
        brevDataAvslagArbeidsgiver.arbeidsland = "Test";
        brevDataAvslagArbeidsgiver.vilkårbegrunnelser121 = new HashSet<>();
        brevDataAvslagArbeidsgiver.vilkårbegrunnelser121VesentligVirksomhet = new HashSet<>();
        return brevDataAvslagArbeidsgiver;
    }


    private static Brevbestilling lagBrevbestillingAvslagArbeidsgiver() {
        Brevbestilling.Builder builder = new Brevbestilling.Builder();
        builder.medDokumentType(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER);
        builder.medBehandling(lagBehandling());
        return builder.build();
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse sadr = new StrukturertAdresse();
        sadr.landkode = "NL";
        sadr.poststed = "Sted";
        sadr.postnummer = "1234";
        sadr.gatenavn = "Gate";
        sadr.husnummer = "1";
        return sadr;
    }

    private DokumentService lagDokumentService(BrevDataByggerVelger brevdatabyggervelger) throws Exception {
        Aktoer aktør = lagAktør(BRUKER);
        Behandling behandling = lagBehandling();
        BehandlingService behandlingService = mockBehandlingService(behandling);
        TpsFasade tpsFasade = mockTpsFasade(aktør);
        Avklartefakta arbeidsgiverFaktum = lagAvklarteFakta(VIRKSOMHET, ORGNR);
        Avklartefakta yrkesgruppeFaktum = lagAvklarteFakta(YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name(), null);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(Arrays.asList(arbeidsgiverFaktum,
            lagAvklarteFakta(ARBEIDSLAND, "SE"),
            yrkesgruppeFaktum));
        BehandlingsresultatRepository behandlingsresultatRepository = mockBehandlingsresultatRepo(behandlingsresultat);
        AvklarteFaktaRepository avklarteFaktaRepository = mockAvklarteFaktaRepository(arbeidsgiverFaktum, yrkesgruppeFaktum);
        AvklartefaktaDtoKonverterer faktaKonverterer = new AvklartefaktaDtoKonverterer();
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer);

        if (brevdatabyggervelger == null) {
            brevdatabyggervelger = lagBrevdataByggerVelger(avklartefaktaService);
        }

        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        BrevDataService brevDataService = new BrevDataService(tpsFasade, behandlingsresultatRepository, utenlandskMyndighetRepository);
        BrevmottakerService brevmottakerService = new BrevmottakerService(mock(KontaktopplysningService.class), avklarteVirksomheterService, mock(UtenlandskMyndighetService.class), behandlingsresultatService);
        return new DokumentService(behandlingService, brevDataService, dokSysFasade,
            prosessinstansService, brevmottakerService, brevdatabyggervelger, lagBrevinput(tpsFasade, avklartefaktaService));
    }

    private BrevdataGrunnlagFactory lagBrevinput(TpsFasade tpsFasade, AvklartefaktaService avklartefaktaService) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        KodeverkRegister kodeverkRegister = mockKodeverkRegister();
        KodeverkService kodeverkService = new KodeverkService(kodeverkRegister);
        EregFasade eregFasade = mockEregFasade();
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(eregFasade, tpsFasade);
        AvklarteVirksomheterSystemService avklarteVirksomheterSystemService = new AvklarteVirksomheterSystemService(avklartefaktaService, registerOppslagService);
        BrevDataGrunnlag dataGrunnlag = new BrevDataGrunnlag(lagBehandling(), kodeverkService,avklarteVirksomheterSystemService, avklartefaktaService);
        BrevdataGrunnlagFactory brevdataGrunnlagFactory = mock(BrevdataGrunnlagFactory.class);
        when(brevdataGrunnlagFactory.av(any())).thenReturn(dataGrunnlag);
        return brevdataGrunnlagFactory;
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(GSAKSNUMMER);
        Set<Aktoer> aktører = new HashSet<>(Arrays.asList(lagAktør(BRUKER),
            lagAktør(REPRESENTANT)));
        fagsak.setAktører(aktører);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.KLAGE);
        behandling.setId(BEHANDLINGSID);
        SoeknadDokument dok = new SoeknadDokument();
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        dok.foretakUtland.add(foretakUtland);
        dok.juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        dok.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = Collections.singletonList(ORGNR);
        dok.oppholdUtland.oppholdslandkoder.add("DK");
        Saksopplysning søknad = lagSaksopplysning(SaksopplysningType.SØKNAD, dok);
        Saksopplysning personopplysninger = lagSaksopplysning(SaksopplysningType.PERSOPL, lagPersonDokument());
        behandling.setSaksopplysninger(new HashSet<>(Arrays.asList(søknad, personopplysninger)));
        return behandling;
    }

    private static PersonDokument lagPersonDokument() {
        PersonDokument resultat = new PersonDokument();
        resultat.kjønn = lagKjoennsType();
        resultat.statsborgerskap = new Land(Land.BELGIA);
        resultat.fornavn = "For";
        resultat.etternavn = "Etter";
        resultat.sammensattNavn = "For Etter";
        resultat.fødselsdato = LocalDate.ofYearDay(1900, 1);
        resultat.bostedsadresse = lagBostedsadresse();
        return resultat;
    }

    private static KjoennsType lagKjoennsType() {
        KjoennsType kjønn = new KjoennsType();
        kjønn.setKode("K");
        return kjønn;
    }

    private static AvklarteFaktaRepository mockAvklarteFaktaRepository(Avklartefakta arbeidsgiverFaktum, Avklartefakta yrkesgruppeFaktum) {
        AvklarteFaktaRepository avklarteFaktaRepository = mock(AvklarteFaktaRepository.class);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(BEHANDLINGSID, YRKESGRUPPE)).thenReturn(Optional.of(yrkesgruppeFaktum));
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(BEHANDLINGSID,
            VIRKSOMHET,
            "TRUE")).thenReturn(Collections.singleton(arbeidsgiverFaktum));
        return avklarteFaktaRepository;
    }

    private static BrevDataByggerVelger lagBrevdataByggerVelger(AvklartefaktaService avklartefaktaService) {
        AnmodningsperiodeService anmodningsperiodeService = mock(AnmodningsperiodeService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        VilkaarsresultatService vilkaarsresultatService = mock(VilkaarsresultatService.class);
        JoarkService joarkService = mock(JoarkService.class);
        BehandlingsresultatService behandlingsresultatService = mock(BehandlingsresultatService.class);
        BehandlingsgrunnlagService behandlingsgrunnlagService = mock(BehandlingsgrunnlagService.class);
        LandvelgerService landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService, behandlingsgrunnlagService, vilkaarsresultatRepository);
        UtpekingService utpekingService = mock(UtpekingService.class);
        return new BrevDataByggerVelger(anmodningsperiodeService, avklartefaktaService, lovvalgsperiodeService,
            utenlandskMyndighetService, vilkaarsresultatRepository, vilkaarsresultatService, joarkService, landvelgerService, utpekingService);
    }

    private BrevDataByggerVelger lagBrevdatabyggerVelgerMock() throws FunksjonellException, TekniskException {
        return lagBrevdatabyggerVelgerMock(new BrevbestillingDto());
    }

    private BrevDataByggerVelger lagBrevdatabyggerVelgerMock(BrevbestillingDto bestillingDto) throws FunksjonellException, TekniskException {
        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse = mock(BrevDataByggerInnvilgelse.class);
        BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver = mock(BrevDataByggerAvslagArbeidsgiver.class);
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);

        BrevDataByggerVelger brevdatabyggervelger = mock(BrevDataByggerVelger.class);
        if (bestillingDto != null) {
            if (bestillingDto.mottaker == ARBEIDSGIVER) {
                when(brevdatabyggervelger.hent(any(), eq(bestillingDto))).thenReturn(brevDataByggerAvslagArbeidsgiver);
                when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(lagBrevDataAvslagArbeidsgiver());
            } else {
                when(brevdatabyggervelger.hent(eq(INNVILGELSE_YRKESAKTIV))).thenReturn(brevDataByggerInnvilgelse);
                when(brevDataByggerInnvilgelse.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
                when(brevdatabyggervelger.hent(eq(AVSLAG_ARBEIDSGIVER))).thenReturn(brevDataByggerAvslagArbeidsgiver);
                when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(lagBrevDataAvslagArbeidsgiver());
                when(brevdatabyggervelger.hent(any(), eq(bestillingDto))).thenReturn(brevDataByggerVedlegg);
                when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
            }
        } else {
            when(brevdatabyggervelger.hent(any(), any())).thenReturn(brevDataByggerVedlegg);
            when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
        }

        return brevdatabyggervelger;
    }

    private static EregFasade mockEregFasade() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        EregFasade eregFasade = mock(EregFasade.class);
        OrganisasjonDokument orgDok = new OrganisasjonDokument();
        orgDok.setNavn(Collections.singletonList("Virker av og til"));
        OrganisasjonsDetaljer organisasjonDetaljer = new OrganisasjonsDetaljer();
        SemistrukturertAdresse adresse = new SemistrukturertAdresse();
        adresse.setLandkode("NO");
        adresse.setAdresselinje1("Gate 1");
        adresse.setPostnr("1234");
        Periode gyldighetsperiode = new Periode(LocalDate.now().minusYears(10), LocalDate.now().plusYears(10));
        adresse.setGyldighetsperiode(gyldighetsperiode);
        organisasjonDetaljer.forretningsadresse = Collections.singletonList(adresse);
        orgDok.setOrganisasjonDetaljer(organisasjonDetaljer);
        orgDok.setOrgnummer(ORGNR);
        when(eregFasade.hentOrganisasjon(ORGNR)).thenReturn(lagSaksopplysning(SaksopplysningType.ORG, orgDok));
        return eregFasade;
    }

    private static KodeverkRegister mockKodeverkRegister() {
        KodeverkRegister kodeverkRegister = mock(KodeverkRegister.class);
        Kodeverk kodeverk = new Kodeverk("", Collections.emptyMap());
        when(kodeverkRegister.hentKodeverk(FellesKodeverk.POSTNUMMER.getNavn())).thenReturn(kodeverk);
        return kodeverkRegister;
    }

    private static Behandlingsresultat lagBehandlingsresultat(List<Avklartefakta> faktaliste) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(new HashSet<>(faktaliste));
        Lovvalgsperiode periode = lagLovvalgsperiode();
        List<Lovvalgsperiode> perioder = Collections.singletonList(periode);
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(perioder));
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setFom(LocalDate.now());
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.NO);
        periode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        return periode;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo(Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        when(behandlingsresultatRepository.findById(BEHANDLINGSID)).thenReturn(Optional.of(behandlingsresultat));
        return behandlingsresultatRepository;
    }

    private static TpsFasade mockTpsFasade(Aktoer aktør) throws IkkeFunnetException {
        TpsFasade tpsFasade = mock(TpsFasade.class);
        when(tpsFasade.hentIdentForAktørId(anyString()))
            .thenReturn(String.format("IDENT%s", aktør.getAktørId()));
        return tpsFasade;
    }

    private static BehandlingService mockBehandlingService(Behandling behandling) throws IkkeFunnetException {
        BehandlingService behandlingService = mock(BehandlingService.class);
        when(behandlingService.hentBehandling(eq(BEHANDLINGSID))).thenReturn(behandling);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLINGSID))).thenReturn(behandling);
        when(behandlingService.hentBehandling(not(eq(BEHANDLINGSID)))).thenThrow(new IkkeFunnetException("Behandling finnes ikke."));
        when(behandlingService.hentBehandlingUtenSaksopplysninger(not(eq(BEHANDLINGSID)))).thenThrow(new IkkeFunnetException("Behandling finnes ikke."));
        return behandlingService;
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning søknad = new Saksopplysning();
        søknad.setType(type);
        søknad.setDokument(dokument);
        return søknad;
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatyper type, String subjekt) {
        return lagAvklarteFakta(type, "TRUE", subjekt);
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatyper type, String fakta, String subjekt) {
        Avklartefakta arbeidsgiverFaktum = new Avklartefakta();
        arbeidsgiverFaktum.setSubjekt(subjekt);
        arbeidsgiverFaktum.setType(type);
        arbeidsgiverFaktum.setFakta(fakta);
        return arbeidsgiverFaktum;
    }

    private static Aktoer lagAktør(Aktoersroller type) {
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(type.name() + idTeller++);
        aktør.setAktørId("123");
        aktør.setOrgnr("999");
        aktør.setRolle(type);
        if (type == REPRESENTANT) {
            aktør.setRepresenterer(Representerer.BRUKER);
        }
        return aktør;
    }
}
