package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Test;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatype.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public final class DokumentServiceTest {

    private static final long BEHANDLINGSID = 13;
    private static final long GSAKSNUMMER = 321L;
    private static final String ORGNR = "123456789";

    private static long idTeller = 1;
    private final DokumentService instans;
    private final DokSysFasade dokSysFasade;

    public DokumentServiceTest() throws Exception {
        this.dokSysFasade = mock(DokSysFasade.class);
        this.instans = lagDokumentService(dokSysFasade, null);
    }

    @Test
    public final void produserInnvilgelsesbrevFunker() throws Exception {
        BrevData brevData = lagBrevData(Aktoersroller.BRUKER);
        instans.produserDokument(BEHANDLINGSID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, brevData);
        verify(dokSysFasade).produserIkkeredigerbartDokument(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevutkastFunker() throws Exception {
        OidcProfile oidcProfile = mock(OidcProfile.class);
        when(oidcProfile.getSubject()).thenReturn("testbruker");
        Pac4jAuthenticationToken auth = new Pac4jAuthenticationToken(Collections.singletonList(oidcProfile));
        SecurityContextHolder.getContext().setAuthentication(auth);
        BrevbestillingDto brevbestilling = lagBrevBestillingDto();

        DokumentService dokumentServiceMedMockVelger = lagDokumentService(dokSysFasade, lagBrevdatabyggerVelgerMock(brevbestilling));
        byte[] resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevMedFullmektigFunker() throws Exception {
        BrevData brevDataDto = lagBrevData(Aktoersroller.REPRESENTANT);
        instans.produserDokument(BEHANDLINGSID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, brevDataDto);
    }

    @Test
    public final void produserMangelbrevISaksflyt() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto();
        instans.produserDokumentISaksflyt(BEHANDLINGSID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, brevbestilling);
    }

    private static BrevbestillingDto lagBrevBestillingDto() {
        BrevbestillingDto brevbestilling = new BrevbestillingDto();
        brevbestilling.mottaker = Aktoersroller.BRUKER;
        return brevbestilling;
    }

    @Test
    public final void produserMangelbrevISaksflytUtenBrevdata() throws Exception {
        instans.produserDokumentISaksflyt(BEHANDLINGSID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, null);
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflytUtenBehandlingKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(~BEHANDLINGSID,
                Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, lagBrevBestillingDto()));
        assertThat(unntak).isInstanceOfAny(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserUkjentDokumenttypeISaksflytKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(BEHANDLINGSID,
                Produserbaredokumenter.MELDING_HENLAGT_SAK, lagBrevBestillingDto()));
        assertThat(unntak).isInstanceOfAny(FunksjonellException.class).hasNoCause().hasMessageContaining("er ikke støttet");
    }

    @Test
    public final void produserDokumentUtenBehandlingKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(~BEHANDLINGSID, Produserbaredokumenter.ATTEST_A1, lagBrevData(Aktoersroller.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserDokumentUtenDokumenttypeKasterUnntak() {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(BEHANDLINGSID, null, lagBrevData(Aktoersroller.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(IllegalArgumentException.class).hasNoCause().hasMessageContaining("Ingen gyldig");
    }

    private static BrevData lagBrevData(Aktoersroller mottakerRolle) {
        BrevDataA1 brevDataA1 = new BrevDataA1();
        brevDataA1.mottaker = mottakerRolle;
        Virksomhet arbeidsgiver = new Virksomhet("Virker av og til", "987654321", lagStrukturertAdresse());
        brevDataA1.norskeVirksomheter = new ArrayList<>(Arrays.asList(arbeidsgiver, arbeidsgiver));
        brevDataA1.bostedsadresse = lagBostedsadresse();
        brevDataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevDataA1.selvstendigeForetak = Collections.emptySet();
        brevDataA1.utenlandskeVirksomheter = Collections.emptyList();
        brevDataA1.person = lagPersonDokument();
        brevDataA1.arbeidssteder = new ArrayList<>();
        brevDataA1.hovedvirksomhet = arbeidsgiver;
        BrevDataVedlegg vedlegg = new BrevDataVedlegg("Saksbehandler");
        vedlegg.mottaker = mottakerRolle;
        vedlegg.brevDataA1 = brevDataA1;
        return vedlegg;
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse sadr = new StrukturertAdresse();
        sadr.landKode = "NL";
        sadr.poststed = "Sted";
        sadr.postnummer = "1234";
        sadr.gatenavn = "Gate";
        sadr.husnummer = "1";
        return sadr;
    }

    private static DokumentService lagDokumentService(DokSysFasade dokSysFasade, BrevDataByggerVelger brevdatabyggervelger) throws Exception {
        Aktoer aktør = lagAktør(Aktoersroller.BRUKER);
        Behandling behandling = lagBehandling();
        BehandlingRepository behandlingRepository = mockBehandlingRepository(behandling);
        TpsFasade tpsFasade = mockTpsFasade(aktør);
        Avklartefakta arbeidsgiverFaktum = lagAvklarteFakta(AVKLARTE_ARBEIDSGIVER, ORGNR);
        Avklartefakta yrkesgruppeFaktum = lagAvklarteFakta(YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name(), null);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(Arrays.asList(arbeidsgiverFaktum,
                lagAvklarteFakta(ARBEIDSLAND, "SE"),
                yrkesgruppeFaktum));
        BehandlingsresultatRepository behandlingsresultatRepository = mockBehandlingsresultatRepo(behandlingsresultat);
        AvklarteFaktaRepository avklarteFaktaRepository = mockAvklarteFaktaRepository(arbeidsgiverFaktum, yrkesgruppeFaktum);
        if (brevdatabyggervelger == null) {
            brevdatabyggervelger = lagBrevdataByggerVelger(tpsFasade, avklarteFaktaRepository, behandlingsresultatRepository);
        }

        BrevDataService brevDataService = new BrevDataService(tpsFasade, behandlingsresultatRepository);
        return new DokumentService(behandlingRepository, mock(FagsakRepository.class), brevDataService, dokSysFasade, mock(JoarkFasade.class),
                mock(ProsessinstansService.class), brevdatabyggervelger);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(GSAKSNUMMER);
        Set<Aktoer> aktører = new HashSet<>(Arrays.asList(lagAktør(Aktoersroller.BRUKER),
                lagAktør(Aktoersroller.REPRESENTANT)));
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
        dok.oppholdUtland.oppholdslandKoder.add("DK");
        Saksopplysning søknad = lagSaksopplysning(SaksopplysningType.SØKNAD, dok);
        Saksopplysning personopplysninger = lagSaksopplysning(SaksopplysningType.PERSONOPPLYSNING, lagPersonDokument());
        behandling.setSaksopplysninger(new HashSet<>(Arrays.asList(søknad, personopplysninger)));
        return behandling;
    }

    private static PersonDokument lagPersonDokument() {
        PersonDokument resultat = new PersonDokument();
        resultat.kjønn = lagKjoennsType();
        resultat.statsborgerskap = new Land(Land.BELGIA);
        resultat.fornavn = "For";
        resultat.etternavn = "Etter";
        resultat.fødselsdato = LocalDate.ofYearDay(1900, 1);
        resultat.bostedsadresse = lagBostedsadresse();
        return resultat;
    }

    private static KjoennsType lagKjoennsType() {
        KjoennsType kjønn = new KjoennsType();
        kjønn.setKode("K");
        return kjønn;
    }

    private static Bostedsadresse lagBostedsadresse() {
        Bostedsadresse badr = new Bostedsadresse();
        badr.setLand(new Land(Land.BELGIA));
        badr.setPoststed("Sted");
        badr.setPostnr("1234");
        Gateadresse gadr = lagGateAdresse();
        badr.setGateadresse(gadr);
        return badr;
    }

    private static Gateadresse lagGateAdresse() {
        Gateadresse gadr = new Gateadresse();
        gadr.setGatenavn("Gate");
        gadr.setGatenummer(1);
        gadr.setHusbokstav("A");
        gadr.setHusnummer(123);
        return gadr;
    }

    private static AvklarteFaktaRepository mockAvklarteFaktaRepository(Avklartefakta arbeidsgiverFaktum, Avklartefakta yrkesgruppeFaktum) {
        AvklarteFaktaRepository avklarteFaktaRepository = mock(AvklarteFaktaRepository.class);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(BEHANDLINGSID, YRKESGRUPPE)).thenReturn(Optional.of(yrkesgruppeFaktum));
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(BEHANDLINGSID,
                AVKLARTE_ARBEIDSGIVER,
                "TRUE")).thenReturn(Collections.singleton(arbeidsgiverFaktum));
        return avklarteFaktaRepository;
    }

    private static BrevDataByggerVelger lagBrevdataByggerVelger(TpsFasade tpsFasade, AvklarteFaktaRepository avklarteFaktaRepository, BehandlingsresultatRepository behandlingsresultatRepository)
            throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        AvklartefaktaDtoKonverterer faktaKonverterer = new AvklartefaktaDtoKonverterer();
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer);
        EregFasade eregFasade = mockEregFasade();
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(eregFasade, tpsFasade);
        KodeverkRegister kodeverkRegister = mockKodeverkRegister();
        KodeverkService kodeverkService = new KodeverkService(kodeverkRegister);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        JoarkService joarkService = mock(JoarkService.class);
        return new BrevDataByggerVelger(avklartefaktaService, registerOppslagService, kodeverkService, lovvalgsperiodeService, utenlandskMyndighetRepository,
                vilkaarsresultatRepository, joarkService);
    }

    private BrevDataByggerVelger lagBrevdatabyggerVelgerMock(BrevbestillingDto bestillingDto) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);

        BrevDataByggerVelger brevdatabyggervelger = mock(BrevDataByggerVelger.class);
        if (bestillingDto != null) {
            when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevData(bestillingDto.mottaker));
            when(brevdatabyggervelger.hent(any(), eq(bestillingDto))).thenReturn(brevDataByggerVedlegg);
        }
        else {
            when(brevdatabyggervelger.hent(any())).thenReturn(brevDataByggerVedlegg);
            when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(lagBrevData(Aktoersroller.BRUKER));
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
        when(eregFasade.hentOrganisasjon(ORGNR)).thenReturn(lagSaksopplysning(SaksopplysningType.ORGANISASJON, orgDok));
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
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setFom(LocalDate.now());
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.NO);
        List<Lovvalgsperiode> perioder = Collections.singletonList(periode);
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(perioder));
        return behandlingsresultat;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo(Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        when(behandlingsresultatRepository.findById(BEHANDLINGSID)).thenReturn(Optional.of(behandlingsresultat));
        return behandlingsresultatRepository;
    }

    private static TpsFasade mockTpsFasade(Aktoer aktør) throws IkkeFunnetException {
        TpsFasade tpsFasade = mock(TpsFasade.class);
        when(tpsFasade.hentIdentForAktørId(eq(aktør.getAktørId())))
            .thenReturn(String.format("IDENT%s", aktør.getAktørId()));
        return tpsFasade;
    }

    private static BehandlingRepository mockBehandlingRepository(Behandling behandling) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findById(eq(BEHANDLINGSID))).thenReturn(Optional.of(behandling));
        when(behandlingRepository.findWithSaksopplysningerById(eq(BEHANDLINGSID))).thenReturn(behandling);
        return behandlingRepository;
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning søknad = new Saksopplysning();
        søknad.setType(type);
        søknad.setDokument(dokument);
        return søknad;
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatype type, String subjekt) {
        return lagAvklarteFakta(type, "TRUE", subjekt);
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatype type, String fakta, String subjekt) {
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
        aktør.setRolle(type);
        return aktør;
    }
}
