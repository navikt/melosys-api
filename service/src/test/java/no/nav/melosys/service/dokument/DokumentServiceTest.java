package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerInnvilgelse;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagSystemService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

final class DokumentServiceTest {
    private static final long BEHANDLINGSID = 13L;
    private static final long GSAKSNUMMER = 321L;
    private static final String ORGNR = "123456789";

    private static long idTeller = 1L;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final DoksysFasade dokSysFasade;
    private final DokumentService dokumentService;
    private final BehandlingsresultatService behandlingsresultatService;

    public DokumentServiceTest() {
        avklarteVirksomheterService = mock(AvklarteVirksomheterService.class);
        dokSysFasade = mock(DoksysFasade.class);
        behandlingsresultatService = mock(BehandlingsresultatService.class);
        dokumentService = lagDokumentService(null);
    }

    @Test
    void produserInnvilgelsesbrev_medFullmektig_senderTilBrukerOgFullmektig() {
        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock());
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build();
        dokumentServiceMedMockVelger.produserDokument(INNVILGELSE_YRKESAKTIV, Mottaker.av(BRUKER), BEHANDLINGSID, brevbestilling);
        verify(dokSysFasade, times(2)).produserIkkeredigerbartDokument(any(Dokumentbestilling.class));
    }

    @Test
    void produser_avslagArbeidsgiver_funker() {
        DoksysBrevbestilling brevbestilling = lagBrevbestillingAvslagArbeidsgiver();
        Set<String> arbeidsgivendeOrgnumre = Collections.singleton("987654321");
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any(Behandling.class))).thenReturn(arbeidsgivendeOrgnumre);
        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock());
        dokumentServiceMedMockVelger.produserDokument(AVSLAG_ARBEIDSGIVER, Mottaker.av(ARBEIDSGIVER), BEHANDLINGSID, brevbestilling);
        verify(dokSysFasade).produserIkkeredigerbartDokument(any(Dokumentbestilling.class));
    }

    @Test
    void produserUtkast_innvilgelsesBrev_funker() {
        BrevbestillingRequest brevbestilling = lagBrevBestillingDto(INNVILGELSE_YRKESAKTIV, BRUKER);

        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling));
        byte[] resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(Dokumentbestilling.class));
    }

    @Test
    void produserUtkast_avslagArbeidsgiver_funker() {
        BrevbestillingRequest brevbestilling = lagBrevBestillingDto(AVSLAG_ARBEIDSGIVER, ARBEIDSGIVER);
        Set<String> arbeidsgivendeOrgnumre = Collections.singleton("987654321");
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any(Behandling.class))).thenReturn(arbeidsgivendeOrgnumre);

        DokumentService dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling));
        byte[] resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(Dokumentbestilling.class));
    }

    @Test
    void produserDokumentUtenBehandlingKasterUnntak() {
        Throwable unntak = catchThrowable(() -> dokumentService.produserDokument(ATTEST_A1, Mottaker.av(ARBEIDSGIVER), ~BEHANDLINGSID, new DoksysBrevbestilling.Builder().build()));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    void produserDokumentUtenDokumenttypeKasterUnntak() {
        Throwable unntak = catchThrowable(() -> dokumentService.produserDokument(null, Mottaker.av(ARBEIDSGIVER), BEHANDLINGSID, new DoksysBrevbestilling.Builder().build()));
        assertThat(unntak).isInstanceOf(IllegalArgumentException.class).hasNoCause().hasMessageContaining("Ingen gyldig");
    }

    private static BrevbestillingRequest lagBrevBestillingDto(Produserbaredokumenter produserbartdokument, Aktoersroller rolle) {
        return new BrevbestillingRequest.Builder()
            .medProduserbardokument(produserbartdokument)
            .medMottaker(rolle)
            .build();
    }

    private static BrevData lagBrevDataInnvilgelse() {
        BrevDataA1 brevDataA1 = new BrevDataA1();
        AvklartVirksomhet arbeidsgiver = new AvklartVirksomhet("Virker av og til", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevDataA1.hovedvirksomhet = arbeidsgiver;
        brevDataA1.bostedsadresse = lagStrukturertAdresse();
        brevDataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevDataA1.bivirksomheter = Collections.emptyList();
        brevDataA1.person = lagPersonopplysninger();
        brevDataA1.arbeidssteder = new ArrayList<>();
        brevDataA1.arbeidsland = new ArrayList<>();
        BrevDataInnvilgelse brevdataInnvilgelse = new BrevDataInnvilgelse(new BrevbestillingRequest(), "SAKSBEHANDLER");
        brevdataInnvilgelse.vedleggA1 = brevDataA1;
        brevdataInnvilgelse.hovedvirksomhet = arbeidsgiver;
        brevdataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevdataInnvilgelse.avklartMaritimType = Maritimtyper.SKIP;
        brevdataInnvilgelse.arbeidsland = "Norway";
        brevdataInnvilgelse.setAnmodningsperiodesvar(lagAnmodningsperiodeSvarInnvilgelse());
        brevdataInnvilgelse.trygdemyndighetsland = "Denmark";
        brevdataInnvilgelse.avklarteMedfolgendeBarn = lagAvklarteMedfølgendeBarn();

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


    private static DoksysBrevbestilling lagBrevbestillingAvslagArbeidsgiver() {
        DoksysBrevbestilling.Builder builder = new DoksysBrevbestilling.Builder();
        builder.medProduserbartDokument(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER);
        builder.medBehandling(lagBehandling());
        return builder.build();
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse sadr = new StrukturertAdresse();
        sadr.setLandkode("NL");
        sadr.setPoststed("Sted");
        sadr.setPostnummer("1234");
        sadr.setGatenavn("Gate");
        sadr.setHusnummerEtasjeLeilighet("1");
        return sadr;
    }

    private DokumentService lagDokumentService(BrevDataByggerVelger brevdatabyggervelger) {
        Aktoer aktør = lagAktør(BRUKER);
        Behandling behandling = lagBehandling();
        BehandlingService behandlingService = mockBehandlingService(behandling);
        PersondataFasade persondataFasade = mockPersondataFasade(aktør);
        Avklartefakta arbeidsgiverFaktum = lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, ORGNR);
        Avklartefakta yrkesgruppeFaktum = lagAvklarteFakta(Avklartefaktatyper.YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name(), null);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(Arrays.asList(arbeidsgiverFaktum,
            lagAvklarteFakta(Avklartefaktatyper.ARBEIDSLAND, "SE"),
            yrkesgruppeFaktum));
        BehandlingsresultatRepository behandlingsresultatRepository = mockBehandlingsresultatRepo(behandlingsresultat);
        AvklarteFaktaRepository avklarteFaktaRepository = mockAvklarteFaktaRepository(arbeidsgiverFaktum, yrkesgruppeFaktum);
        AvklartefaktaDtoKonverterer faktaKonverterer = new AvklartefaktaDtoKonverterer();
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer);

        if (brevdatabyggervelger == null) {
            brevdatabyggervelger = lagBrevdataByggerVelger(avklartefaktaService);
        }

        SaksbehandlerService saksbehandlerService = mock(SaksbehandlerService.class);
        when(saksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Bob Lastname");
        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        BrevDataService brevDataService = new BrevDataService(behandlingsresultatRepository, persondataFasade,
            saksbehandlerService, utenlandskMyndighetRepository);
        BrevmottakerService brevmottakerService = new BrevmottakerService(mock(KontaktopplysningService.class),
            avklarteVirksomheterService,
            mock(UtenlandskMyndighetService.class),
            behandlingsresultatService,
            mock(TrygdeavgiftsberegningService.class),
            mock(LovvalgsperiodeService.class), behandlingService);
        return new DokumentService(behandlingService, brevDataService, dokSysFasade, brevmottakerService, brevdatabyggervelger, lagBrevinput(persondataFasade, avklartefaktaService));
    }

    private BrevdataGrunnlagFactory lagBrevinput(PersondataFasade persondataFasade, AvklartefaktaService avklartefaktaService) {
        KodeverkRegister kodeverkRegister = mockKodeverkRegister();
        KodeOppslag kodeOppslag = mock(KodeOppslag.class);
        KodeverkService kodeverkService = new KodeverkService(kodeverkRegister, kodeOppslag);
        EregFasade eregFasade = mockEregFasade();
        OrganisasjonOppslagSystemService registerOppslagService = new OrganisasjonOppslagSystemService(eregFasade);
        AvklarteVirksomheterService avklarteVirksomheterSystemService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), mock(KodeverkService.class));
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(lagBehandling()).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        BrevDataGrunnlag dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterSystemService, avklartefaktaService, persondata);
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

        Soeknad søknad = new Soeknad();
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        søknad.foretakUtland.add(foretakUtland);
        søknad.juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = Collections.singletonList(ORGNR);
        søknad.oppholdUtland.oppholdslandkoder.add("DK");

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        Saksopplysning personopplysninger = lagSaksopplysning(SaksopplysningType.PERSOPL, lagPersonDokument());
        behandling.setSaksopplysninger(Set.of(personopplysninger));
        return behandling;
    }

    private static PersonDokument lagPersonDokument() {
        PersonDokument resultat = new PersonDokument();
        resultat.setKjønn(lagKjoennsType());
        resultat.setStatsborgerskap(new Land(Land.BELGIA));
        resultat.setFornavn("For");
        resultat.setEtternavn("Etter");
        resultat.setSammensattNavn("For Etter");
        resultat.setFødselsdato(LocalDate.ofYearDay(1900, 1));
        resultat.setBostedsadresse(lagBostedsadresse());
        return resultat;
    }

    private static KjoennsType lagKjoennsType() {
        return new KjoennsType("K");
    }

    private static AvklarteFaktaRepository mockAvklarteFaktaRepository(Avklartefakta arbeidsgiverFaktum, Avklartefakta yrkesgruppeFaktum) {
        AvklarteFaktaRepository avklarteFaktaRepository = mock(AvklarteFaktaRepository.class);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(BEHANDLINGSID, Avklartefaktatyper.YRKESGRUPPE)).thenReturn(Optional.of(yrkesgruppeFaktum));
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(BEHANDLINGSID,
            Avklartefaktatyper.VIRKSOMHET,
            "TRUE")).thenReturn(Collections.singleton(arbeidsgiverFaktum));
        return avklarteFaktaRepository;
    }

    private static BrevDataByggerVelger lagBrevdataByggerVelger(AvklartefaktaService avklartefaktaService) {
        AnmodningsperiodeService anmodningsperiodeService = mock(AnmodningsperiodeService.class);
        BehandlingsgrunnlagService behandlingsgrunnlagService = mock(BehandlingsgrunnlagService.class);
        BehandlingsresultatService behandlingsresultatService = mock(BehandlingsresultatService.class);
        JoarkService joarkService = mock(JoarkService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        LandvelgerService landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService,
            behandlingsgrunnlagService);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        UtpekingService utpekingService = mock(UtpekingService.class);
        VilkaarsresultatService vilkaarsresultatService = mock(VilkaarsresultatService.class);
        PersondataFasade persondataFasade = mock(PersondataFasade.class);
        return new BrevDataByggerVelger(anmodningsperiodeService, avklartefaktaService, joarkService,
            landvelgerService, lovvalgsperiodeService, saksopplysningerService, utenlandskMyndighetService,
            utpekingService, vilkaarsresultatRepository, vilkaarsresultatService, persondataFasade, behandlingsgrunnlagService);
    }

    private BrevDataByggerVelger lagBrevdatabyggerVelgerMock() {
        return lagBrevdatabyggerVelgerMock(new BrevbestillingRequest());
    }

    private BrevDataByggerVelger lagBrevdatabyggerVelgerMock(BrevbestillingRequest bestillingDto) {
        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse = mock(BrevDataByggerInnvilgelse.class);
        BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver = mock(BrevDataByggerAvslagArbeidsgiver.class);
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);

        BrevDataByggerVelger brevdatabyggervelger = mock(BrevDataByggerVelger.class);
        if (bestillingDto != null) {
            if (bestillingDto.getMottaker() == ARBEIDSGIVER) {
                when(brevdatabyggervelger.hent(any(), any(BrevbestillingRequest.class))).thenReturn(brevDataByggerAvslagArbeidsgiver);
                when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(lagBrevDataAvslagArbeidsgiver());
            } else {
                when(brevdatabyggervelger.hent(any(), any())).thenReturn(brevDataByggerVedlegg);
                when(brevdatabyggervelger.hent(eq(INNVILGELSE_YRKESAKTIV), any())).thenReturn(brevDataByggerInnvilgelse);
                when(brevdatabyggervelger.hent(eq(AVSLAG_ARBEIDSGIVER), any())).thenReturn(brevDataByggerAvslagArbeidsgiver);
                when(brevDataByggerInnvilgelse.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
                when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(lagBrevDataAvslagArbeidsgiver());
                when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
            }
        } else {
            when(brevdatabyggervelger.hent(any(), any())).thenReturn(brevDataByggerVedlegg);
            when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevDataInnvilgelse());
        }

        return brevdatabyggervelger;
    }

    private static EregFasade mockEregFasade() {
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

    private static PersondataFasade mockPersondataFasade(Aktoer aktør) {
        PersondataFasade persondataFasade = mock(PersondataFasade.class);
        when(persondataFasade.hentFolkeregisterident(anyString()))
            .thenReturn(String.format("IDENT%s", aktør.getAktørId()));
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        return persondataFasade;
    }

    private static BehandlingService mockBehandlingService(Behandling behandling) {
        BehandlingService behandlingService = mock(BehandlingService.class);
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLINGSID)).thenReturn(behandling);
        when(behandlingService.hentBehandling(BEHANDLINGSID)).thenReturn(behandling);
        when(behandlingService.hentBehandlingMedSaksopplysninger(not(eq(BEHANDLINGSID)))).thenThrow(new IkkeFunnetException("Behandling finnes ikke."));
        when(behandlingService.hentBehandling(not(eq(BEHANDLINGSID)))).thenThrow(new IkkeFunnetException("Behandling finnes ikke."));
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
