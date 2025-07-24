package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.w3c.dom.Element;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrevDataServiceTest {
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private SaksbehandlerService saksbehandlerService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private BrevDataService service;

    private static final String FNR = "Fnr";
    private static final String ORGNR = "Org-Nr";
    private static final String REP_ORGNR = "REP_Org-Nr";
    private static final String REP_FNR = "REP_Fnr";
    private static final String AKTØRID = "Aktør-Id";
    private static final String INSTITUSJON_ID = "HR:Zxcd";

    private static final String sammensattNavn = "ALTFOR SAMMENSATT";

    @BeforeEach
    public void setUp() {
        service = spy(new BrevDataService(behandlingsresultatRepository, persondataFasade, saksbehandlerService, utenlandskMyndighetRepository));

        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(new Behandlingsresultat()));
        when(saksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Joe Moe");
        when(persondataFasade.hentFolkeregisterident(any())).thenReturn(FNR);
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn(sammensattNavn);
        lagUtenlandskMyndighet();
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.setNavn("navn");
        myndighet.setGateadresse1("gateadresse 123");
        myndighet.setGateadresse2("institusjon ABC");
        myndighet.setLand("HR");
        when(utenlandskMyndighetRepository.findByLandkode(Land_iso2.HR)).thenReturn(Optional.of(myndighet));
        return myndighet;
    }

    @Test
    void lagA1_tilUtenlandskMyndighet() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        Aktoer aktoerMyndighet = lagAktoerMyndighet();
        behandling.getFagsak().leggTilAktør(aktoerMyndighet);
        BrevDataVedlegg brevData = new BrevDataVedlegg("Z123456");
        UtenlandskMyndighet myndighet = lagUtenlandskMyndighet();
        no.nav.melosys.domain.brev.Mottaker mottakerMyndighet = lagMottakerMyndighet();
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(ATTEST_A1, mottakerMyndighet, null,
            behandling, brevData);

        assertThat(metadata.getBrukerID()).isEqualTo(FNR);
        assertThat(metadata.getMottakerID()).isEqualTo(INSTITUSJON_ID);
        assertThat(metadata.getUtenlandskMyndighet()).isEqualTo(myndighet);
        assertThat(metadata.getBrukerNavn()).isEqualTo(sammensattNavn);

        Element element = service.lagBrevXML(ATTEST_A1, mottakerMyndighet, null, behandling, brevData);

        assertThat(element).isNotNull();
    }

    @Test
    void lagBrevXML_tilUtenlandskMyndighet() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevDataVedlegg brevData = new BrevDataVedlegg("Z123456");
        no.nav.melosys.domain.brev.Mottaker mottakerNorskMyndighet = no.nav.melosys.domain.brev.Mottaker.av(NorskMyndighet.SKATTEETATEN);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(ATTEST_A1, mottakerNorskMyndighet, null,
            behandling, brevData);

        assertThat(metadata.getMottakerID()).isEqualTo(mottakerNorskMyndighet.getOrgnr());

        Element element = service.lagBrevXML(ATTEST_A1, mottakerNorskMyndighet, null, behandling, brevData);

        assertThat(element).isNotNull();
    }

    private static Aktoer lagAktoerMyndighet() {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        myndighet.setInstitusjonID(INSTITUSJON_ID);
        return myndighet;
    }

    private static no.nav.melosys.domain.brev.Mottaker lagMottakerMyndighet() {
        var myndighet = new no.nav.melosys.domain.brev.Mottaker();
        myndighet.setRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        myndighet.setInstitusjonID(INSTITUSJON_ID);
        return myndighet;
    }

    @Test
    void hentUtenlandskTrygdemyndighetFraMottaker() {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        mottaker.setInstitusjonID("DE:TEST");
        UtenlandskMyndighet tyskMyndighet = new UtenlandskMyndighet();
        tyskMyndighet.setInstitusjonskode("TEST");
        when(utenlandskMyndighetRepository.findByLandkode(Land_iso2.DE)).thenReturn(Optional.of(tyskMyndighet));

        UtenlandskMyndighet utenlandskMyndighet = service.hentUtenlandskTrygdemyndighetFraMottaker(mottaker);

        assertThat(utenlandskMyndighet.getInstitusjonskode()).isEqualTo(tyskMyndighet.getInstitusjonskode());
    }

    private static no.nav.melosys.domain.brev.Mottaker lagMottaker(Mottakerroller rolle) {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(rolle);
        switch (rolle) {
            case BRUKER -> mottaker.setAktørId(AKTØRID);
            case ARBEIDSGIVER, VIRKSOMHET, NORSK_MYNDIGHET -> mottaker.setOrgnr(ORGNR);
            case UTENLANDSK_TRYGDEMYNDIGHET -> mottaker.setInstitusjonID("HR:987");
            case FULLMEKTIG -> throw new IllegalArgumentException("Bruk lagMottakerFullmektig() for fullmekitg mottaker");
        }
        return mottaker;
    }

    private static no.nav.melosys.domain.brev.Mottaker lagMottakerFullmektig(Aktoertype mottakerType) {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.FULLMEKTIG);
        switch (mottakerType) {
            case PERSON -> mottaker.setPersonIdent(REP_FNR);
            case ORGANISASJON -> mottaker.setOrgnr(REP_ORGNR);
            default -> throw new IllegalArgumentException("Mottakertype må være person eller organisasjon");
        }
        return mottaker;
    }

    @Test
    void lagMetadataForInnvilgelsesbrevAngirDokTypeLikInnvilgelseYrkesaktiv() {
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Mottakerroller.BRUKER);
    }

    @Test
    void lagMetadataForInnvilgelseArbeidsgiverBrevAngirDokTypeLikArbeidsgiver() {
        testLagDokumentMetadata(INNVILGELSE_ARBEIDSGIVER, Mottakerroller.ARBEIDSGIVER);
    }

    @Test
    void avklarMottakerId_fullmektigOgKontaktOpplysningFinnes_kontaktOpplysningForFullmektigBrukes() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        behandling.getFagsak().leggTilAktør(hentFullmektigOrgAktør());

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456", "test", null);
        var mottaker = lagMottakerFullmektig(Aktoertype.ORGANISASJON);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData);

        assertThat(metadata.getBrukerID()).isEqualTo(FNR);
        assertThat(metadata.getMottakerID()).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData);

        assertThat(metadata.getBrukerID()).isEqualTo(FNR);
        assertThat(metadata.getMottakerID()).isEqualTo(REP_ORGNR);
    }

    @Test
    void lagBestillingMetadata_medBrukerMottakerOgBrukerUtenAdresseIRegister_skalHaBrukernavnOgPostadresse() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456", null, null);

        var mottaker = lagMottaker(Mottakerroller.BRUKER);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData);
        assertThat(metadata.getPostadresse().getGatenavn()).isEqualTo("Strukturert Gate");
        assertThat(metadata.getBrukerNavn()).isEqualTo(sammensattNavn);
        assertThat(metadata.getBerik()).isFalse();
    }

    @Test
    void lagBestillingMetadata_medBrukerMedAdresseIRegister_skalIkkeHaBrukerNavnEllerPostAdresse() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456", null, null);
        var mottaker = lagMottaker(Mottakerroller.BRUKER);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData);
        assertThat(metadata.getPostadresse()).isNull();
        assertThat(metadata.getBrukerNavn()).isNull();
        assertThat(metadata.getBerik()).isTrue();
    }

    @Test
    void lagBestillingMetadata_medUtenlandskMyndighet_skalUtfyllesMedBrukerNavn() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456", null, null);

        var mottaker = lagMottaker(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData);
        assertThat(metadata.getPostadresse()).isNull();
        assertThat(metadata.getBrukerNavn()).isEqualTo(sammensattNavn);
        assertThat(metadata.getUtenlandskMyndighet()).isNotNull();
        assertThat(metadata.getBerik()).isFalse();
    }

    @Test
    void lagBestillingMetadata_medNorskMyndighet_skalSeteOrgnrSomMottakerID() {
        var mottaker = lagMottaker(Mottakerroller.NORSK_MYNDIGHET);


        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            null, lagBehandling(lagSøknadDokument()), new BrevData("Z123456", null, null));


        assertThat(metadata.getMottakerID()).isEqualTo(mottaker.getOrgnr());
    }

    @Test
    void avklarMottakerId_ingenFullmektigForArbeidsgiverOgKontaktOpplysningFinnes_kontaktOpplysningBrukes() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456", null, null);
        var mottaker = lagMottaker(Mottakerroller.ARBEIDSGIVER);
        brevData.setFritekst("Test");

        Behandling behandling = lagBehandling(lagSøknadDokument());
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData);
        assertThat(metadata.getMottakerID()).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData);
        assertThat(metadata.getMottakerID()).isEqualTo(ORGNR);
        assertThat(metadata.getBerik()).isTrue();
    }

    @Test
    void lagMottaker_bruker_riktigeVerdier() {
        when(persondataFasade.hentPerson(AKTØRID)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.BRUKER);
        mottaker.setAktørId(AKTØRID);

        var brevMottaker = service.lagMottaker(mottaker, null);

        Mottaker expectedBrevMottaker = new Person();
        expectedBrevMottaker.setTypeKode(AktoerType.PERSON);
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);
        expectedBrevMottaker.setId(FNR);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setBerik(true);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_arbeidsgiver_riktigeVerdier() {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.ARBEIDSGIVER);
        mottaker.setOrgnr(ORGNR);

        var brevMottaker = service.lagMottaker(mottaker, null);

        Mottaker expectedBrevMottaker = new Organisasjon();
        expectedBrevMottaker.setId(ORGNR);
        expectedBrevMottaker.setTypeKode(AktoerType.ORGANISASJON);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_trygdemyndighetUtenlandsk_riktigeVerdier() {
        var mottaker = lagMottakerMyndighet();

        var brevMottaker = service.lagMottaker(mottaker, null);

        var myndighet = lagUtenlandskMyndighet();
        Mottaker expectedBrevMottaker = new Person();
        expectedBrevMottaker.setId(INSTITUSJON_ID);
        expectedBrevMottaker.setTypeKode(AktoerType.PERSON);
        expectedBrevMottaker.setBerik(false);
        expectedBrevMottaker.setNavn(myndighet.getNavn());
        expectedBrevMottaker.setKortNavn(myndighet.getNavn());
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);
        expectedBrevMottaker.setMottakeradresse(new UtenlandskPostadresse()
            .withAdresselinje1(myndighet.getGateadresse1())
            .withAdresselinje2(myndighet.getGateadresse2())
            .withAdresselinje3(myndighet.getPostnummer() + " " + myndighet.getPoststed())
            .withLand(myndighet.getLand()));

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_trygdemyndighetIkkeUtenlandsk_riktigeVerdier() {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        mottaker.setOrgnr(ORGNR);

        var brevMottaker = service.lagMottaker(mottaker, null);

        Mottaker expectedBrevMottaker = new Organisasjon();
        expectedBrevMottaker.setId(ORGNR);
        expectedBrevMottaker.setTypeKode(AktoerType.ORGANISASJON);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_fullmektigPerson_riktigeVerdier() {
        when(persondataFasade.hentPerson(REP_FNR)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.FULLMEKTIG);
        mottaker.setPersonIdent(REP_FNR);

        var brevMottaker = service.lagMottaker(mottaker, null);

        Mottaker expectedBrevMottaker = new Person();
        expectedBrevMottaker.setTypeKode(AktoerType.PERSON);
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);
        expectedBrevMottaker.setId(REP_FNR);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setBerik(true);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_fullmektigOrganisasjon_riktigeVerdier() {
        var mottaker = new no.nav.melosys.domain.brev.Mottaker();
        mottaker.setRolle(Mottakerroller.FULLMEKTIG);
        mottaker.setOrgnr(REP_ORGNR);

        var brevMottaker = service.lagMottaker(mottaker, null);

        Mottaker expectedBrevMottaker = new Organisasjon();
        expectedBrevMottaker.setId(REP_ORGNR);
        expectedBrevMottaker.setTypeKode(AktoerType.ORGANISASJON);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Mottakerroller rolle) {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        testLagDokumentMetadata(doktype, lagMottaker(rolle), rolle);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, no.nav.melosys.domain.brev.Mottaker mottaker, Mottakerroller rolle) {
        DokumentbestillingMetadata resultat = service.lagBestillingMetadata(doktype, mottaker, null,
            lagBehandling(lagSøknadDokument()), lagBrevData());
        DokumentbestillingMetadata forventet = lagDokumentbestillingMetadata(doktype, rolle);
        assertThat(resultat).usingRecursiveComparison().isEqualTo(forventet);
    }

    private static DokumentbestillingMetadata lagDokumentbestillingMetadata(Produserbaredokumenter doktype,
                                                                            Mottakerroller rolle) {
        DokumentbestillingMetadata forventet = new DokumentbestillingMetadata();
        forventet.setBrukerID(FNR);
        forventet.setMottaker(lagMottaker(rolle));
        if (rolle == Mottakerroller.BRUKER) {
            forventet.setMottakerID(FNR);
        } else {
            forventet.setMottakerID(ORGNR);
        }

        forventet.setDokumenttypeID(DokumenttypeIdMapper.hentID(doktype));
        forventet.setFagområde("MED");
        forventet.setJournalsakID(String.valueOf(FagsakTestFactory.GSAK_SAKSNUMMER));
        forventet.setSaksbehandler("TEST");
        forventet.setBerik(true);

        return forventet;
    }

    private static BrevData lagBrevData() {
        BrevData brevDataDto = new BrevData();
        brevDataDto.setSaksbehandler("TEST");
        brevDataDto.setFritekst("Test");
        return brevDataDto;
    }

    private static Behandling lagBehandling(MottatteOpplysningerData mottatteOpplysningerData) {
        Aktoer bruker = new Aktoer();
        bruker.setAktørId(AKTØRID);
        bruker.setRolle(Aktoersroller.BRUKER);

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setOrgnr(ORGNR);
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);

        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().aktører(Set.of(bruker, arbeidsgiver)).build();

        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setId(1L);
        behandling.setRegistrertDato(Instant.now());
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setFagsak(fagsak);

        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(mottatteOpplysningerData);

        return behandling;
    }

    private static Aktoer hentFullmektigOrgAktør() {
        Aktoer aktørArbFullmektig = new Aktoer();
        aktørArbFullmektig.setRolle(Aktoersroller.FULLMEKTIG);
        aktørArbFullmektig.setOrgnr(REP_ORGNR);
        aktørArbFullmektig.setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        return aktørArbFullmektig;
    }

    private Soeknad lagSøknadDokument() {
        Soeknad søknad = new Soeknad();
        søknad.bosted.setOppgittAdresse(lagStrukturertAdresse());
        return søknad;
    }

    private NorskPostadresse lagPlassholderAdresse() {
        return new NorskPostadresse()
            .withAdresselinje1(BrevDataService.PLASSHOLDER_TEKST)
            .withPostnummer(BrevDataService.PLASSHOLDER_POSTNUMMER)
            .withPoststed(BrevDataService.PLASSHOLDER_TEKST)
            .withLand(BrevDataService.PLASSHOLDER_TEKST);
    }

}
