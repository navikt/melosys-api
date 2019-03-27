package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataServiceTest {

    @Mock
    private TpsService tpsService;

    @Mock
    BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Mock
    KontaktopplysningService kontaktopplysningService;

    private BrevDataService service;

    private static final String FNR = "Fnr";
    private static final String ORGNR = "Org-Nr";
    private static final String REP_ORGNR = "REP_Org-Nr";
    private static final String AKTØRID = "Aktør-Id";

    @Before
    public void setUp() throws IkkeFunnetException, TekniskException {
        service = spy(new BrevDataService(tpsService, behandlingsresultatRepository, utenlandskMyndighetRepository, kontaktopplysningService));

        when(tpsService.hentFagsakIdentMedRolleType(any(), any())).thenCallRealMethod();
        when(tpsService.hentIdentForAktørId(any())).thenReturn(FNR);
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(new Behandlingsresultat()));
    }

    @Test
    public void lagA1_tilUtenlandskMyndighet() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Behandling behandling = lagBehandling();
        String institusjonID = "HR:Zxcd";
        behandling.getFagsak().getAktører().add(lagAktoerMyndighet(institusjonID));
        BrevDataVedlegg brevData = new BrevDataVedlegg("Z123456");
        brevData.mottaker = Aktoersroller.MYNDIGHET;
        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.navn = "navn";
        myndighet.gateadresse = "gateadresse 123";
        myndighet.land = "HR";
        when(utenlandskMyndighetRepository.findByLandkode(Landkoder.HR)).thenReturn(myndighet);
        PersonDokument personDokument = new PersonDokument();
        personDokument.sammensattNavn = "Alf Berg";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        when(tpsService.hentPerson(anyString())).thenReturn(saksopplysning);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(ATTEST_A1, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(institusjonID);
        assertThat(metadata.utenlandskMyndighet).isEqualTo(myndighet);

        Element element = service.lagBrevXML(ATTEST_A1, behandling, brevData);

        assertThat(element).isNotNull();
    }

    private Aktoer lagAktoerMyndighet(String institusjonID) {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId(institusjonID);
        return myndighet;
    }

    @Test
    public void lagMyndighet() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagAktoerMyndighet("DE:TEST"));
        UtenlandskMyndighet tyskMyndighet = new UtenlandskMyndighet();
        tyskMyndighet.institusjonskode = "TEST";
        when(utenlandskMyndighetRepository.findByLandkode(Landkoder.DE)).thenReturn(tyskMyndighet);

        UtenlandskMyndighet utenlandskMyndighet = service.hentMyndighetFraSak(fagsak);

        assertThat(utenlandskMyndighet.institusjonskode).isEqualTo(tyskMyndighet.institusjonskode);
    }

    @Test
    public void lagForvaltningsmelding_representantErNull_tilBruker() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Behandling behandling = lagBehandling();

        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.BRUKER;

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);
        assertThat(element).isNotNull();
    }

    @Test
    public void lagForvaltningsmelding_representantIkkeNull_tilRepresentant() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {

        Behandling behandling = lagBehandling();
        behandling.getFagsak().getAktører().add(hentRepresentantAktør());

        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.REPRESENTANT;

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);
        assertThat(element).isNotNull();

    }

    @Test
    public void lagMangelbrevXml_mottakerErbrukerID() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Behandling behandling = lagBehandling();
        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.BRUKER;
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Person.class);
            return mottaker;
        }).when(service).lagMottaker(any(Produserbaredokumenter.class), any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMangelbrevXml_mottakerErArbeidsgiver() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Behandling behandling = lagBehandling();
        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.ARBEIDSGIVER;
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(ORGNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Organisasjon.class);
            return mottaker;
        }).when(service).lagMottaker(any(Produserbaredokumenter.class), any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

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
    public final void avklarMottakerId_finnesRepresentantOgKontaktOpplysning_KontaktOpplysningForRepresentantBrukes() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {

        Behandling behandling = lagBehandling();
        behandling.getFagsak().getAktører().add(hentRepresentantAktør());

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.REPRESENTANT;
        brevData.fritekst = "Test";
        when(kontaktopplysningService.hentKontaktopplysning(anyString(), anyString())).thenReturn(Optional.of(kontaktopplysning));

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        when(kontaktopplysningService.hentKontaktopplysning(anyString(), anyString())).thenReturn(Optional.empty());
        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.brukerID).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REP_ORGNR);
    }

    @Test
    public final void avklarMottakerId_IngenRepresentantForArbeidsgiverKontaktOpplysningFinnes_KontaktOpplysningBrukes() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {

        Behandling behandling = lagBehandling();

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktopplysningID(new KontaktopplysningID("MELTEST-1", "999"));
        kontaktopplysning.setKontaktNavn("brev motakker");
        kontaktopplysning.setKontaktOrgnr("KONTAKTORG_999");

        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.ARBEIDSGIVER;
        brevData.fritekst = "Test";
        when(kontaktopplysningService.hentKontaktopplysning(anyString(), anyString())).thenReturn(Optional.of(kontaktopplysning));
        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.mottakerID).isEqualTo("KONTAKTORG_999");

        when(kontaktopplysningService.hentKontaktopplysning(anyString(), anyString())).thenReturn(Optional.empty());
        metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.mottakerID).isEqualTo(ORGNR);
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws Exception {
        DokumentbestillingMetadata resultat = service.lagBestillingMetadata(doktype, lagBehandling(), lagBrevData(rolle));
        DokumentbestillingMetadata forventet = lagDokumentbestillingMetadata(doktype, rolle);
        assertThat(resultat).isEqualToComparingFieldByFieldRecursively(forventet);
    }

    private static DokumentbestillingMetadata lagDokumentbestillingMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws TekniskException {
        DokumentbestillingMetadata forventet = new DokumentbestillingMetadata();
        forventet.brukerID = FNR;
        forventet.mottakersRolle = rolle;
        if (rolle == Aktoersroller.BRUKER) {
            forventet.mottakerID = FNR;
            forventet.utledRegisterInfo = true;
        } else if (rolle == Aktoersroller.ARBEIDSGIVER) {
            forventet.mottakerID = ORGNR;
            forventet.utledRegisterInfo = true;
        } else {
            forventet.mottakerID = ORGNR;
            forventet.utledRegisterInfo = false;
        }

        forventet.dokumenttypeID = DokumenttypeIdMapper.hentID(doktype);
        forventet.fagområde = "MED";
        forventet.journalsakID = "123";
        forventet.saksbehandler = "TEST";

        return forventet;
    }

    private static BrevData lagBrevData(Aktoersroller rolle) {
        BrevData brevDataDto = new BrevData();
        brevDataDto.saksbehandler = "TEST";
        brevDataDto.mottaker = rolle;
        brevDataDto.fritekst = "Test";
        return brevDataDto;
    }

    private static Behandling lagBehandling() {
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

        return behandling;
    }

    private static Aktoer hentRepresentantAktør() {
        Aktoer aktørArbRep = new Aktoer();
        aktørArbRep.setRolle(Aktoersroller.REPRESENTANT);
        aktørArbRep.setOrgnr(REP_ORGNR);
        aktørArbRep.setRepresenterer(Representerer.ARBEIDSGIVER);
        return aktørArbRep;
    }
}
