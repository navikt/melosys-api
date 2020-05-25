package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataServiceTest {
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private SaksbehandlerService saksbehandlerService;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private BrevDataService service;

    private static final String FNR = "Fnr";
    private static final String ORGNR = "Org-Nr";
    private static final String REP_ORGNR = "REP_Org-Nr";
    private static final String AKTØRID = "Aktør-Id";

    private static final String sammensattNavn = "ALTFOR SAMMENSATT";

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        service = spy(new BrevDataService(behandlingsresultatRepository, saksbehandlerService, tpsFasade, utenlandskMyndighetRepository));

        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(new Behandlingsresultat()));
        when(saksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Joe Moe");
        when(tpsFasade.hentIdentForAktørId(any())).thenReturn(FNR);
        when(tpsFasade.hentSammensattNavn(anyString())).thenReturn(sammensattNavn);
        lagUtenlandskMyndighet();
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.navn = "navn";
        myndighet.gateadresse = "gateadresse 123";
        myndighet.land = "HR";
        when(utenlandskMyndighetRepository.findByLandkode(Landkoder.HR)).thenReturn(Optional.of(myndighet));
        return myndighet;
    }

    @Test
    public void lagA1_tilUtenlandskMyndighet() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        String institusjonID = "HR:Zxcd";
        Aktoer aktoerMyndighet = lagAktoerMyndighet(institusjonID);
        behandling.getFagsak().getAktører().add(aktoerMyndighet);
        BrevDataVedlegg brevData = new BrevDataVedlegg("Z123456");
        UtenlandskMyndighet myndighet = lagUtenlandskMyndighet();
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(ATTEST_A1, aktoerMyndighet, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(institusjonID);
        assertThat(metadata.utenlandskMyndighet).isEqualTo(myndighet);
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);

        Element element = service.lagBrevXML(ATTEST_A1, aktoerMyndighet, null, behandling, brevData);

        assertThat(element).isNotNull();
    }

    private static Aktoer lagAktoerMyndighet(String institusjonID) {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId(institusjonID);
        return myndighet;
    }

    @Test
    public void lagMyndighetFraAktoer() throws TekniskException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.MYNDIGHET);
        aktoer.setInstitusjonId("DE:TEST");
        UtenlandskMyndighet tyskMyndighet = new UtenlandskMyndighet();
        tyskMyndighet.institusjonskode = "TEST";
        when(utenlandskMyndighetRepository.findByLandkode(Landkoder.DE)).thenReturn(Optional.of(tyskMyndighet));

        UtenlandskMyndighet utenlandskMyndighet = service.hentMyndighetFraAktoer(aktoer);

        assertThat(utenlandskMyndighet.institusjonskode).isEqualTo(tyskMyndighet.institusjonskode);
    }

    @Test
    public void lagForvaltningsmelding_representantErNull_tilBruker() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    private static Aktoer lagAktør(Aktoersroller type) {
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(type.name());
        aktør.setAktørId(AKTØRID);
        aktør.setInstitusjonId("HR:987");
        if (type == Aktoersroller.REPRESENTANT) {
            aktør.setOrgnr(REP_ORGNR);
        } else {
            aktør.setOrgnr(ORGNR);
        }
        aktør.setRolle(type);
        return aktør;
    }

    @Test
    public void lagForvaltningsmelding_representantIkkeNull_tilRepresentant() throws TekniskException, FunksjonellException {

        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        behandling.getFagsak().getAktører().add(hentRepresentantAktør());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottaker = lagAktør(Aktoersroller.REPRESENTANT);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();

    }

    @Test
    public void lagMangelbrevXml_mottakerErbrukerID() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottakerAktør = lagAktør(Aktoersroller.BRUKER);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottakerAktør, null, behandling, brevData);

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
    public void lagMangelbrevXml_mottakerErArbeidsgiver() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        Aktoer mottakerAktør = lagAktør(Aktoersroller.ARBEIDSGIVER);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottakerAktør, null, behandling, brevData);

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
    public void lagBrevXml_medBrukerMedAdresseIRegister_skalBerikes() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        brevData.fritekst = "Test";

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    public void lagBrevXml_medBrukerUtenAdresseIRegister_skalIkkeBerikes() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysningerUtenAdresseIRegister(), lagSøknadDokument());

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        brevData.fritekst = "Test";

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    public void lagMetadataForInnvilgelsesbrevAngirDokTypeLikInnvilgelseYrkesaktiv() throws Exception {
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER);
    }

    @Test
    public void lagMetadataForMangelbrevAngirDokTypeLikMangelbrev() throws Exception {
        testLagDokumentMetadata(MELDING_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER);
    }

    @Test
    public void lagMetadataForMangelbrevAngirDokTypeLikHenleggelse() throws Exception {
        testLagDokumentMetadata(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER);
    }

    @Test
    public void lagMetadataForInnvilgelseArbeidsgiverBrevAngirDokTypeLikArbeidsgiver() throws Exception {
        testLagDokumentMetadata(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER);
    }

    @Test
    public final void avklarMottakerId_representantOgKontaktOpplysningFinnes_kontaktOpplysningForRepresentantBrukes() throws TekniskException, FunksjonellException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        behandling.getFagsak().getAktører().add(hentRepresentantAktør());

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        Aktoer mottaker = lagAktør(Aktoersroller.REPRESENTANT);
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, kontaktopplysning, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);
    }

    private Collection<Saksopplysning> lagSaksopplysningerUtenAdresseIRegister() {
        Collection<Saksopplysning> saksopplysninger = new HashSet<>();
        saksopplysninger.add(lagPersonsaksopplysning(new PersonDokument()));
        return saksopplysninger;
    }

    @Test
    public void lagBestillingMetadata_medBrukerMottakerOgBrukerUtenAdresseIRegister_skalHaBrukernavnOgPostadresse() throws FunksjonellException, TekniskException {
        Collection<Saksopplysning> saksopplysninger = lagSaksopplysningerUtenAdresseIRegister();

        Behandling behandling = lagBehandling(saksopplysninger, lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(metadata.postadresse.gatenavn).isEqualTo("Strukturert Gate");
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);
        assertThat(metadata.berik).isFalse();
    }

    @Test
    public void lagBestillingMetadata_medBrukerMedAdresseIRegister_skalIkkeHaBrukerNavnEllerPostAdresse() throws FunksjonellException, TekniskException {
        Collection<Saksopplysning> saksopplysninger = lagSaksopplysninger();
        Behandling behandling = lagBehandling(saksopplysninger, lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");

        Aktoer mottaker = lagAktør(Aktoersroller.BRUKER);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(metadata.postadresse).isNull();
        assertThat(metadata.brukerNavn).isNull();
        assertThat(metadata.berik).isTrue();
    }

    @Test
    public void lagBestillingMetadata_medUtenlandskMyndighet_skalUtfyllesMedBrukerNavn() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        BrevData brevData = new BrevData("Z123456");

        Aktoer mottaker = lagAktør(Aktoersroller.MYNDIGHET);
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(metadata.postadresse).isNull();
        assertThat(metadata.brukerNavn).isEqualTo(sammensattNavn);
        assertThat(metadata.utenlandskMyndighet).isNotNull();
        assertThat(metadata.berik).isFalse();
    }

    @Test
    public final void avklarMottakerId_ingenRepresentantForArbeidsgiverOgKontaktOpplysningFinnes_kontaktOpplysningBrukes() throws TekniskException, FunksjonellException {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        Aktoer mottaker = lagAktør(Aktoersroller.ARBEIDSGIVER);
        brevData.fritekst = "Test";

        Behandling behandling = lagBehandling(lagSaksopplysninger(), lagSøknadDokument());
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, kontaktopplysning, behandling, brevData);
        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, mottaker, null, behandling, brevData);
        assertThat(metadata.mottakerID).isEqualTo(ORGNR);
        assertThat(metadata.berik).isTrue();
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws Exception {
        testLagDokumentMetadata(doktype, lagAktør(rolle), rolle);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoer mottaker, Aktoersroller rolle) throws Exception {
        DokumentbestillingMetadata resultat = service.lagBestillingMetadata(
            doktype, mottaker, null, lagBehandling(lagSaksopplysninger(), lagSøknadDokument()), lagBrevData()
        );
        DokumentbestillingMetadata forventet = lagDokumentbestillingMetadata(doktype, rolle);
        assertThat(resultat).usingRecursiveComparison().isEqualTo(forventet);
    }

    private static DokumentbestillingMetadata lagDokumentbestillingMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws TekniskException {
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

    private static Behandling lagBehandling(Collection<Saksopplysning> saksopplysninger, BehandlingsgrunnlagData behandlingsgrunnlagData) {
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

        behandling.getSaksopplysninger().addAll(saksopplysninger);
        return behandling;
    }

    private Collection<Saksopplysning> lagSaksopplysninger() {
        Collection<Saksopplysning> saksopplysninger = new HashSet<>();

        PersonDokument person = new PersonDokument();
        person.bostedsadresse = lagBostedsadresse();
        saksopplysninger.add(lagPersonsaksopplysning(person));
        return saksopplysninger;
    }

    private static Aktoer hentRepresentantAktør() {
        Aktoer aktørArbRep = new Aktoer();
        aktørArbRep.setRolle(Aktoersroller.REPRESENTANT);
        aktørArbRep.setOrgnr(REP_ORGNR);
        aktørArbRep.setRepresenterer(Representerer.ARBEIDSGIVER);
        return aktørArbRep;
    }

    private SoeknadDokument lagSøknadDokument() {
        SoeknadDokument søknad = new SoeknadDokument();
        søknad.bosted.oppgittAdresse = lagStrukturertAdresse();
        return søknad;
    }
}
