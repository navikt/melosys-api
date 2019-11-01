package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.*;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.dok.tjenester.journalfoerinngaaende.response.Mangler;
import no.nav.dok.tjenester.journalfoerinngaaende.response.Status;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumentInformasjonMangler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journalfoeringsbehov;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.JournalpostMangler;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.KorrespendansePart;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JoarkServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private JoarkService joarkService;

    @Mock
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    @Mock
    private JournalConsumer journalConsumer;
    @Mock
    private JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;
    @Mock
    private JournalpostapiConsumer journalpostapiConsumer;
    @Captor
    private ArgumentCaptor<PutJournalpostRequest> oppdaterJournalpostCaptor;
    @Captor
    private ArgumentCaptor<PutDokumentRequest> oppdaterDokumentCaptor;
    @Captor
    private ArgumentCaptor<PostLogiskVedleggRequest> logiskVedleggCaptor;

    @Before
    public void setUp() {
        this.joarkService = new JoarkService(inngaaendeJournalConsumer, journalConsumer, journalfoerInngaaendeConsumer, journalpostapiConsumer);
    }

    @Test
    public void hentKjerneJournalpostListe() throws SikkerhetsbegrensningException, IntegrasjonException, HentKjerneJournalpostListeUgyldigInput, HentKjerneJournalpostListeSikkerhetsbegrensning, DatatypeConfigurationException {
        Long arkivSakID = 1L;
        HentKjerneJournalpostListeResponse response = new HentKjerneJournalpostListeResponse();
        no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost journalpost = new no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost();
        ArkivSak arkivSak = new ArkivSak();
        arkivSak.setArkivSakId("123");
        journalpost.setGjelderArkivSak(arkivSak);
        journalpost.setForsendelseMottatt(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now()));
        journalpost.setForsendelseJournalfoert(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now().plusDays(2)));
        KorrespendansePart korrespendansePart = new KorrespendansePart();
        String partID = "partID";
        String partNavn = "Ola N";
        korrespendansePart.setKorrespondansepartId(partID);
        korrespendansePart.setKorrespondansepartNavn(partNavn);
        journalpost.setKorrespondansePart(korrespendansePart);
        DetaljertDokumentinformasjon dokumentInfo = new DetaljertDokumentinformasjon();
        String dokID = "dokID_1";
        String tittel = "Tittel 1";
        dokumentInfo.setDokumentId(dokID);
        dokumentInfo.setTittel(tittel);
        journalpost.setHoveddokument(dokumentInfo);
        Journalposttyper type = new Journalposttyper();
        type.setValue("I");
        journalpost.setJournalposttype(type);
        response.getJournalpostListe().add(journalpost);
        when(journalConsumer.hentKjerneJournalpostListe(any())).thenReturn(response);

        List<Journalpost> journalpostListe = joarkService.hentKjerneJournalpostListe(arkivSakID);
        assertThat(journalpostListe.size()).isEqualTo(1);

        Journalpost journalpost1 = journalpostListe.get(0);
        assertThat(journalpost1.getArkivSakId()).isEqualTo("123");
        assertThat(journalpost1.getHoveddokument().getDokumentId()).isEqualTo(dokID);
        assertThat(journalpost1.getHoveddokument().getTittel()).isEqualTo(tittel);
        assertThat(journalpost1.getJournalposttype()).isEqualTo(Journalposttype.INN);
        assertThat(journalpost1.getForsendelseMottatt()).isNotNull();
        assertThat(journalpost1.getForsendelseJournalfoert()).isNotNull();
        assertThat(journalpost1.getKorrespondansepartId()).isEqualTo(partID);
        assertThat(journalpost1.getKorrespondansepartNavn()).isEqualTo(partNavn);
    }


    @Test
    public void konverterTilJournalfoeringmangler() {
        JournalpostMangler input = new JournalpostMangler();
        input.setArkivSak(Journalfoeringsbehov.MANGLER);
        input.setAvsenderId(Journalfoeringsbehov.MANGLER_IKKE);
        input.setAvsenderNavn(Journalfoeringsbehov.MANGLER);
        input.setBruker(Journalfoeringsbehov.MANGLER);
        input.setForsendelseInnsendt(Journalfoeringsbehov.MANGLER_IKKE);
        DokumentInformasjonMangler dokumentInformasjonMangler = new DokumentInformasjonMangler();
        dokumentInformasjonMangler.setDokumentkategori(Journalfoeringsbehov.MANGLER);
        dokumentInformasjonMangler.setTittel(Journalfoeringsbehov.MANGLER_IKKE);
        input.setHoveddokument(dokumentInformasjonMangler);
        input.setInnhold(Journalfoeringsbehov.MANGLER);
        input.setTema(Journalfoeringsbehov.MANGLER);

        List<JournalfoeringMangel> journalfoeringMangler = joarkService.konverterTilJournalfoeringmangler(input);

        assertThat(journalfoeringMangler).isNotNull();
        assertThat(journalfoeringMangler).isNotEmpty();
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.ARKIVSAK);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.AVSENDERID);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.AVSENDERNAVN);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.BRUKER);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.FORSENDELSEINNSENDT);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        assertThat(journalfoeringMangler).doesNotContain(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.INNHOLD);
        assertThat(journalfoeringMangler).contains(JournalfoeringMangel.TEMA);
    }

    @Test
    public void oppdaterJournalpost_påkrevdeVerdierUtfylt() throws Exception {
        String tittel = "tittel";
        Map<String, String> vedleggMedTitler = new HashMap<>();
        String fysiskVedleggTittel = "Fysisk vedlegg";
        vedleggMedTitler.put("vedleggDokID", fysiskVedleggTittel);
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(1L)
            .medHovedDokumentID("1234").medBrukerID("12345")
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.ORGANISASJON)
            .medTittel(tittel).medFysiskeVedlegg(vedleggMedTitler)
            .medLogiskeVedleggTitler(Arrays.asList("dok1", "dok2")).medDokumentkategori(true).build();
        joarkService.oppdaterJournalpost("123",journalpostOppdatering, false);

        verify(journalfoerInngaaendeConsumer).oppdaterJournalpost(oppdaterJournalpostCaptor.capture(), anyString());
        PutJournalpostRequest request = oppdaterJournalpostCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.getTittel()).isEqualTo(tittel);
        assertThat(request.getAvsender()).isNotNull();
        assertThat(request.getAvsender().getNavn()).isNotNull();
        assertThat(request.getAvsender().getAvsenderType()).isEqualTo(Avsender.AvsenderType.ORGANISASJON);

        assertThat(request.getBruker()).isNotNull();
        assertThat(request.getBruker().getIdentifikator()).isNotNull();
        assertThat(request.getBruker().getBrukerType()).isNotNull();

        assertThat(request.getArkivSak()).isNotNull();
        assertThat(request.getArkivSak().getArkivSakId()).isNotNull();
        assertThat(request.getArkivSak().getArkivSakSystem()).isNotNull();

        verify(journalfoerInngaaendeConsumer, times(2)).oppdaterDokument(oppdaterDokumentCaptor.capture(), anyString(), anyString());
        List<PutDokumentRequest> dokumentRequest = oppdaterDokumentCaptor.getAllValues();
        assertThat(dokumentRequest.size()).isEqualTo(2);
        assertThat(dokumentRequest.get(0).getDokumentKategori()).isEqualTo(DokumentKategoriKode.IS.getKode());
        assertThat(dokumentRequest.get(0).getTittel()).isEqualTo(tittel);
        assertThat(dokumentRequest.get(1).getTittel()).isEqualTo(fysiskVedleggTittel);

        verify(journalfoerInngaaendeConsumer, times(2)).leggTilLogiskVedlegg(logiskVedleggCaptor.capture(), anyString(), anyString());
        List<PostLogiskVedleggRequest> logiskVedleggRequest = logiskVedleggCaptor.getAllValues();
        assertThat(logiskVedleggRequest.size()).isEqualTo(2);
        assertThat(logiskVedleggRequest.get(0).getTittel()).isEqualTo("dok1");
        assertThat(logiskVedleggRequest.get(1).getTittel()).isEqualTo("dok2");
    }

    @Test
    public void oppdaterJournalpost_utenVedlegg_fungerer() throws Exception {
        String tittel = "tittel";
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(1L)
            .medBrukerID("12345").medHovedDokumentID("1234")
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.PERSON).medTittel(tittel).medFysiskeVedlegg(null)
            .medLogiskeVedleggTitler(null).medDokumentkategori(true).build();
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, false);

        verify(journalfoerInngaaendeConsumer).oppdaterJournalpost(oppdaterJournalpostCaptor.capture(), anyString());
        PutJournalpostRequest request = oppdaterJournalpostCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.getTittel()).isEqualTo(tittel);
        assertThat(request.getAvsender()).isNotNull();
        assertThat(request.getAvsender().getNavn()).isNotNull();

        assertThat(request.getBruker()).isNotNull();
        assertThat(request.getBruker().getIdentifikator()).isNotNull();
        assertThat(request.getBruker().getBrukerType()).isNotNull();

        assertThat(request.getArkivSak()).isNotNull();
        assertThat(request.getArkivSak().getArkivSakId()).isNotNull();
        assertThat(request.getArkivSak().getArkivSakSystem()).isNotNull();

        verify(journalfoerInngaaendeConsumer).oppdaterDokument(oppdaterDokumentCaptor.capture(), anyString(), anyString());
        PutDokumentRequest dokumentRequest = oppdaterDokumentCaptor.getValue();
        assertThat(dokumentRequest.getDokumentKategori()).isEqualTo(DokumentKategoriKode.IS.getKode());
        assertThat(dokumentRequest.getTittel()).isEqualTo(tittel);

        verify(journalfoerInngaaendeConsumer, never()).leggTilLogiskVedlegg(logiskVedleggCaptor.capture(), anyString(), anyString());
    }

    @Test
    public void hentJournalpost_forventJournalpost() throws Exception {
        String arkivsakId = "123arkivsak";
        GetJournalpostResponse getJournalpostResponse = new GetJournalpostResponse();
        getJournalpostResponse.setArkivSak(new ArkivSakNoArkivsakSystemEnum());
        getJournalpostResponse.getArkivSak().setArkivSakId(arkivsakId);

        String mottaksKanal = "EESSI eller NETS";
        getJournalpostResponse.setMottaksKanal(mottaksKanal);

        String brukerId = "123b";
        Bruker bruker = new Bruker();
        bruker.setBrukerType(Bruker.BrukerType.PERSON);
        bruker.setIdentifikator(brukerId);
        getJournalpostResponse.setBrukerListe(Collections.singletonList(bruker));

        String avsenderId = "123avsender";
        Date forsendelseMottatt = new Date();
        Avsender avsender = new Avsender();
        avsender.setIdentifikator(avsenderId);
        getJournalpostResponse.setAvsender(avsender);
        getJournalpostResponse.setForsendelseMottatt(forsendelseMottatt);

        List<Dokument> dokumentListe = new ArrayList<>();
        String dokumentTittel = "titteldok", dokumentId = "123dok", navSkjemaID = "123skjemaID";
        Dokument hoveddokument = new Dokument();
        hoveddokument.setTittel(dokumentTittel);
        hoveddokument.setDokumentId(dokumentId);
        hoveddokument.setNavSkjemaId(navSkjemaID);
        dokumentListe.add(hoveddokument);

        Dokument vedlegg1 = new Dokument();
        vedlegg1.setTittel(dokumentTittel);
        vedlegg1.setDokumentId(dokumentId);
        dokumentListe.add(vedlegg1);

        Dokument vedlegg2 = new Dokument();
        vedlegg2.setTittel(dokumentTittel);
        vedlegg2.setDokumentId(dokumentId);
        dokumentListe.add(vedlegg2);

        getJournalpostResponse.setDokumentListe(dokumentListe);

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(getJournalpostResponse);

        Journalpost journalpost = joarkService.hentJournalpost("1233321");

        assertThat(journalpost).isNotNull();
        assertThat(journalpost.getBrukerId()).isEqualTo(brukerId);
        assertThat(journalpost.getAvsenderId()).isEqualTo(avsenderId);
        assertThat(journalpost.getForsendelseMottatt()).isEqualTo(forsendelseMottatt.toInstant());
        assertThat(journalpost.getHoveddokument().getDokumentId()).isEqualTo(dokumentId);
        assertThat(journalpost.getHoveddokument().getTittel()).isEqualTo(dokumentTittel);
        assertThat(journalpost.getHoveddokument().getNavSkjemaID()).isEqualTo(navSkjemaID);
        assertThat(journalpost.getArkivSakId()).isEqualTo(arkivsakId);
        assertThat(journalpost.getVedleggListe().size()).isEqualTo(2);
        assertThat(journalpost.getMottaksKanal()).isEqualTo(mottaksKanal);
    }

    @Test
    public void ferdigstillJournalpost_journalpostBlirJournalført_ingenException() throws Exception {
        String journalpostId = "123";
        PutJournalpostResponse putJournalpostResponse = new PutJournalpostResponse();
        putJournalpostResponse.setHarEndeligJF(true);
        doReturn(putJournalpostResponse).when(journalfoerInngaaendeConsumer).oppdaterJournalpost(any(), eq(journalpostId));

        joarkService.ferdigstillJournalføring(journalpostId);

        verify(journalfoerInngaaendeConsumer).oppdaterJournalpost(oppdaterJournalpostCaptor.capture(), eq(journalpostId));

        PutJournalpostRequest request = oppdaterJournalpostCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.isForsoekEndeligJF()).isTrue();
        assertThat(request.getJournalfEnhet()).isEqualTo(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
    }

    @Test
    public void ferdigstillJournalpost_journalpostBlirIkkeJournalført_funksjonellExepctionMedManglerKastes() throws Exception {
        String journalpostId = "123";
        PutJournalpostResponse putJournalpostResponse = new PutJournalpostResponse();
        putJournalpostResponse.setHarEndeligJF(false);
        putJournalpostResponse.setJournalpostId(journalpostId);
        Mangler mangler = lagMangler();
        putJournalpostResponse.setMangler(mangler);
        doReturn(putJournalpostResponse).when(journalfoerInngaaendeConsumer).oppdaterJournalpost(any(PutJournalpostRequest.class), eq(journalpostId));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(AllOf.allOf(
            StringContains.containsString("Journalpost 123 har ikke blitt endelig journalført"),
            StringContains.containsString("Tittel: MANGLER"),
            StringContains.containsString("Tema: MANGLER_IKKE")
        ));

        joarkService.ferdigstillJournalføring(journalpostId);

    }

    private Mangler lagMangler() {
        Mangler mangler = new Mangler();
        mangler.setArkivSak(Status.MANGLER_IKKE);
        mangler.setAvsenderNavn(Status.MANGLER_IKKE);
        mangler.setBruker(Status.MANGLER);
        mangler.setTema(Status.MANGLER_IKKE);
        mangler.setTittel(Status.MANGLER);
        return mangler;
    }

    @Test
    public void opprettJournalpost_ikkeValider_forventMetodekall() throws TekniskException {
        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());

        String journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), false);

        verify(journalpostapiConsumer).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    @Test
    public void opprettJournalpost_validerFelt_forventValidert() throws TekniskException {
        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());

        String journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), true);

        verify(journalpostapiConsumer).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    @Test(expected = TekniskException.class)
    public void opprettJournalpost_validerFelt_forventException() throws TekniskException {
        OpprettJournalpost opprettJournalpost = lagOpprettJournalpost();
        opprettJournalpost.setArkivSakId(null);
        String journalpostId = joarkService.opprettJournalpost(opprettJournalpost, true);

        verify(journalpostapiConsumer, never()).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    private OpprettJournalpost lagOpprettJournalpost() {
        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet("9999");
        opprettJournalpost.setTema("tema");
        opprettJournalpost.setMottaksKanal("kanal");
        opprettJournalpost.setInnhold("innhold");
        opprettJournalpost.setArkivSakId("12345");
        opprettJournalpost.setBrukerId("12345678901");
        opprettJournalpost.setKorrespondansepartNavn("navn");
        opprettJournalpost.setKorrespondansepartId("id");
        opprettJournalpost.setKorrespondansepartIdType("UTL_ORG");

        FysiskDokument hoveddokument = new FysiskDokument();
        hoveddokument.setTittel("tittel");
        hoveddokument.setBrevkode("brevkode");

        DokumentVariant dokumentVariant = new DokumentVariant();
        dokumentVariant.setFiltype(DokumentVariant.Filtype.PDFA);
        dokumentVariant.setVariantFormat("ARKIV");
        dokumentVariant.setData("dokument".getBytes());
        hoveddokument.setDokumentVarianter(Collections.singletonList(dokumentVariant));

        hoveddokument.setDokumentKategori(DokumentKategoriKode.SED.name());
        opprettJournalpost.setHoveddokument(hoveddokument);

        return opprettJournalpost;
    }
}