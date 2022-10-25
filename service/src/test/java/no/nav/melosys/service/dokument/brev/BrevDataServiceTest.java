package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
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

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
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
    private final Unleash unleash = new FakeUnleash();

    @BeforeEach
    public void setUp() {
        service = spy(new BrevDataService(behandlingsresultatRepository, persondataFasade, saksbehandlerService,
            utenlandskMyndighetRepository, unleash));

        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(new Behandlingsresultat()));
        when(saksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Joe Moe");
        when(persondataFasade.hentFolkeregisterident(any())).thenReturn(FNR);
        when(persondataFasade.hentSammensattNavn(anyString())).thenReturn(sammensattNavn);
        lagUtenlandskMyndighet();
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.navn = "navn";
        myndighet.gateadresse = "gateadresse 123";
        myndighet.land = "HR";
        when(utenlandskMyndighetRepository.findByLandkode(Land_iso2.HR)).thenReturn(Optional.of(myndighet));
        return myndighet;
    }

    @Test
    void lagA1_tilUtenlandskMyndighet() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        Aktoer aktoerMyndighet = lagAktoerMyndighet();
        behandling.getFagsak().getAktører().add(aktoerMyndighet);
        BrevDataVedlegg brevData = new BrevDataVedlegg("Z123456");
        UtenlandskMyndighet myndighet = lagUtenlandskMyndighet();
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(ATTEST_A1, aktoerMyndighet, null,
            behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(INSTITUSJON_ID);
        assertThat(metadata.utenlandskMyndighet).isEqualTo(myndighet);
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);

        Element element = service.lagBrevXML(ATTEST_A1, aktoerMyndighet, null, behandling, brevData);

        assertThat(element).isNotNull();
    }

    private static Aktoer lagAktoerMyndighet() {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        myndighet.setInstitusjonId(INSTITUSJON_ID);
        return myndighet;
    }

    @Test
    void lagMyndighetFraAktoer() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktoer.setInstitusjonId("DE:TEST");
        UtenlandskMyndighet tyskMyndighet = new UtenlandskMyndighet();
        tyskMyndighet.institusjonskode = "TEST";
        when(utenlandskMyndighetRepository.findByLandkode(Land_iso2.DE)).thenReturn(Optional.of(tyskMyndighet));

        UtenlandskMyndighet utenlandskMyndighet = service.hentMyndighetFraAktoer(aktoer);

        assertThat(utenlandskMyndighet.institusjonskode).isEqualTo(tyskMyndighet.institusjonskode);
    }

    @Test
    void lagMottaker_personUtenRegistrertAdresseGirFunksjonellException() {
        Behandling behandling = lagBehandling(lagSøknadDokumentTomAdresse());
        Aktoer aktoer = lagAktør(Aktoersroller.BRUKER);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        assertThatExceptionOfType(FunksjonellException.class).isThrownBy(
            () -> service.lagMottaker(aktoer, null, behandling)).withMessageContaining(
            Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse());
    }

    @Test
    void lagForvaltningsmelding_representantErNull_tilBruker() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
            mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling,
            brevData);
        assertThat(element).isNotNull();
    }

    private static Aktoer lagAktør(Aktoersroller type) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(type);
        switch (type) {
            case BRUKER -> aktør.setAktørId(AKTØRID);
            case ARBEIDSGIVER, VIRKSOMHET -> aktør.setOrgnr(ORGNR);
            case TRYGDEMYNDIGHET -> aktør.setInstitusjonId("HR:987");
            case REPRESENTANT -> throw new IllegalArgumentException("Bruk lagAktørRepresentant() for fullmekitg mottaker");
        }
        return aktør;
    }

    private static Aktoer lagAktørRepresentant(Aktoertype mottakerType) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.REPRESENTANT);
        switch (mottakerType) {
            case PERSON -> aktør.setPersonIdent(REP_FNR);
            case ORGANISASJON -> aktør.setOrgnr(REP_ORGNR);
            default -> throw new IllegalArgumentException("Mottakertype må være person eller organisasjon");
        }
        return aktør;
    }

    @Test
    void lagForvaltningsmelding_representantErOrganisasjon_tilRepresentant() {

        Behandling behandling = lagBehandling(lagSøknadDokument());
        behandling.getFagsak().getAktører().add(hentRepresentantOrgAktør());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottaker = lagAktørRepresentant(Aktoertype.ORGANISASJON);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
            mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling,
            brevData);
        assertThat(element).isNotNull();

    }

    @Test
    void lagForvaltningsmelding_representantErPerson_tilRepresentant() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Behandling behandling = lagBehandling(lagSøknadDokument());
        behandling.getFagsak().getAktører().add(hentRepresentantPersonAktør());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottaker = lagAktørRepresentant(Aktoertype.PERSON);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
            mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling,
            brevData);
        assertThat(element).isNotNull();

    }

    @Test
    void lagMangelbrevXml_mottakerErbrukerID() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Aktoer mottakerAktør = lagAktør(Aktoersroller.BRUKER);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER,
            mottakerAktør, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Person.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottakerAktør, null, behandling, brevData);

        assertThat(element).isNotNull();
    }

    @Test
    void lagMangelbrevXml_mottakerErArbeidsgiver() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottakerAktør = lagAktør(Aktoersroller.ARBEIDSGIVER);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER,
            mottakerAktør, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(ORGNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Organisasjon.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottakerAktør, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    void lagBrevXml_medBrukerMedAdresseIRegister_skalBerikes() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        brevData.fritekst = "Test";

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    void lagBrevXml_medBrukerUtenAdresseIRegister_skalIkkeBerikes() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        brevData.fritekst = "Test";

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    void lagMetadataForInnvilgelsesbrevAngirDokTypeLikInnvilgelseYrkesaktiv() {
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER);
    }

    @Test
    void lagMetadataForMangelbrevAngirDokTypeLikMangelbrev() {
        testLagDokumentMetadata(MELDING_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER);
    }

    @Test
    void lagMetadataForInnvilgelseArbeidsgiverBrevAngirDokTypeLikArbeidsgiver() {
        testLagDokumentMetadata(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER);
    }

    @Test
    void avklarMottakerId_representantOgKontaktOpplysningFinnes_kontaktOpplysningForRepresentantBrukes() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        behandling.getFagsak().getAktører().add(hentRepresentantOrgAktør());

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        Aktoer mottaker = lagAktørRepresentant(Aktoertype.ORGANISASJON);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker,
            kontaktopplysning, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);
    }

    @Test
    void lagBestillingMetadata_medBrukerMottakerOgBrukerUtenAdresseIRegister_skalHaBrukernavnOgPostadresse() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker,
            null, behandling, brevData);
        assertThat(metadata.postadresse.getGatenavn()).isEqualTo("Strukturert Gate");
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);
        assertThat(metadata.berik).isFalse();
    }

    @Test
    void lagBestillingMetadata_medBrukerMedAdresseIRegister_skalIkkeHaBrukerNavnEllerPostAdresse() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");
        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker,
            null, behandling, brevData);
        assertThat(metadata.postadresse).isNull();
        assertThat(metadata.brukerNavn).isNull();
        assertThat(metadata.berik).isTrue();
    }

    @Test
    void lagBestillingMetadata_medUtenlandskMyndighet_skalUtfyllesMedBrukerNavn() {
        Behandling behandling = lagBehandling(lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");

        Aktoer mottaker = lagAktør(Aktoersroller.TRYGDEMYNDIGHET);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker,
            null, behandling, brevData);
        assertThat(metadata.postadresse).isNull();
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);
        assertThat(metadata.utenlandskMyndighet).isNotNull();
        assertThat(metadata.berik).isFalse();
    }

    @Test
    void avklarMottakerId_ingenRepresentantForArbeidsgiverOgKontaktOpplysningFinnes_kontaktOpplysningBrukes() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        Aktoer mottaker = lagAktør(Aktoersroller.ARBEIDSGIVER);
        brevData.fritekst = "Test";

        Behandling behandling = lagBehandling(lagSøknadDokument());
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker,
            kontaktopplysning, behandling, brevData);
        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(metadata.mottakerID).isEqualTo(ORGNR);
        assertThat(metadata.berik).isTrue();
    }

    @Test
    void lagMottaker_bruker_riktigeVerdier() {
        when(persondataFasade.hentPerson(AKTØRID)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        var mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId(AKTØRID);

        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

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
        var mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.ARBEIDSGIVER);
        mottaker.setOrgnr(ORGNR);

        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

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
        var mottaker = lagAktoerMyndighet();
        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

        var myndighet = lagUtenlandskMyndighet();
        Mottaker expectedBrevMottaker = new Person();
        expectedBrevMottaker.setId(INSTITUSJON_ID);
        expectedBrevMottaker.setTypeKode(AktoerType.PERSON);
        expectedBrevMottaker.setBerik(false);
        expectedBrevMottaker.setNavn(myndighet.navn);
        expectedBrevMottaker.setKortNavn(myndighet.navn);
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);
        expectedBrevMottaker.setMottakeradresse(UtenlandskPostadresse.builder()
            .withAdresselinje1(myndighet.gateadresse)
            .withAdresselinje2(myndighet.postnummer + " " + myndighet.poststed)
            .withAdresselinje3("")
            .withLand(myndighet.land)
            .build());

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    @Test
    void lagMottaker_trygdemyndighetIkkeUtenlandsk_riktigeVerdier() {
        var mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        mottaker.setOrgnr(ORGNR);

        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

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
    void lagMottaker_representantPerson_riktigeVerdier() {
        when(persondataFasade.hentPerson(REP_FNR)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        var mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setPersonIdent(REP_FNR);

        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

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
    void lagMottaker_representantOrganisasjon_riktigeVerdier() {
        var mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setOrgnr(REP_ORGNR);

        var behandling = lagBehandling(new BehandlingsgrunnlagData());

        var brevMottaker = service.lagMottaker(mottaker, null, behandling);

        Mottaker expectedBrevMottaker = new Organisasjon();
        expectedBrevMottaker.setId(REP_ORGNR);
        expectedBrevMottaker.setTypeKode(AktoerType.ORGANISASJON);
        expectedBrevMottaker.setNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setKortNavn(BrevDataService.PLASSHOLDER_TEKST);
        expectedBrevMottaker.setMottakeradresse(lagPlassholderAdresse());
        expectedBrevMottaker.setSpraakkode(Spraakkode.NB);

        assertThat(brevMottaker).isEqualTo(expectedBrevMottaker);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        testLagDokumentMetadata(doktype, lagAktør(rolle), rolle);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoer mottaker, Aktoersroller rolle) {
        DokumentbestillingMetadata resultat = service.lagBestillingMetadata(doktype, mottaker, null,
            lagBehandling(lagSøknadDokument()), lagBrevData());
        DokumentbestillingMetadata forventet = lagDokumentbestillingMetadata(doktype, rolle);
        assertThat(resultat).usingRecursiveComparison().isEqualTo(forventet);
    }

    private static DokumentbestillingMetadata lagDokumentbestillingMetadata(Produserbaredokumenter doktype,
                                                                            Aktoersroller rolle) {
        DokumentbestillingMetadata forventet = new DokumentbestillingMetadata();
        forventet.brukerID = FNR;
        forventet.mottaker = lagAktør(rolle);
        if (rolle == Aktoersroller.BRUKER) {
            forventet.mottakerID = FNR;
        } else {
            forventet.mottakerID = ORGNR;
        }

        forventet.dokumenttypeID = DokumenttypeIdMapper.hentID(doktype);
        forventet.fagområde = "MED";
        forventet.journalsakID = "123";
        forventet.saksbehandler = "TEST";
        forventet.berik = true;

        return forventet;
    }

    private static BrevData lagBrevData() {
        BrevData brevDataDto = new BrevData();
        brevDataDto.saksbehandler = "TEST";
        brevDataDto.fritekst = "Test";
        return brevDataDto;
    }

    private static Behandling lagBehandling(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MOCK-1");
        fagsak.setGsakSaksnummer(123L);

        Aktoer bruker = new Aktoer();
        bruker.setAktørId(AKTØRID);
        bruker.setRolle(Aktoersroller.BRUKER);

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setOrgnr(ORGNR);
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);

        fagsak.setAktører(new HashSet<>(asList(bruker, arbeidsgiver)));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setRegistrertDato(Instant.now());
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        return behandling;
    }

    private static Aktoer hentRepresentantOrgAktør() {
        Aktoer aktørArbRep = new Aktoer();
        aktørArbRep.setRolle(Aktoersroller.REPRESENTANT);
        aktørArbRep.setOrgnr(REP_ORGNR);
        aktørArbRep.setRepresenterer(Representerer.ARBEIDSGIVER);
        return aktørArbRep;
    }

    private static Aktoer hentRepresentantPersonAktør() {
        Aktoer aktørArbRep = new Aktoer();
        aktørArbRep.setRolle(Aktoersroller.REPRESENTANT);
        aktørArbRep.setPersonIdent(REP_FNR);
        aktørArbRep.setRepresenterer(Representerer.ARBEIDSGIVER);
        return aktørArbRep;
    }

    private Soeknad lagSøknadDokument() {
        Soeknad søknad = new Soeknad();
        søknad.bosted.oppgittAdresse = lagStrukturertAdresse();
        return søknad;
    }

    private Soeknad lagSøknadDokumentTomAdresse() {
        Soeknad soeknad = new Soeknad();
        soeknad.bosted.oppgittAdresse = new StrukturertAdresse();
        return soeknad;
    }

    private NorskPostadresse lagPlassholderAdresse() {
        return NorskPostadresse.builder()
            .withAdresselinje1(BrevDataService.PLASSHOLDER_TEKST)
            .withPostnummer(BrevDataService.PLASSHOLDER_POSTNUMMER)
            .withPoststed(BrevDataService.PLASSHOLDER_TEKST)
            .withLand(BrevDataService.PLASSHOLDER_TEKST)
            .build();
    }

}
