package no.nav.melosys.integrasjon.joark;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.Dokument;
import no.nav.dok.tjenester.journalfoerinngaaende.LogiskVedlegg;
import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.melosys.domain.arkiv.DokumentVariant;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumentInformasjonMangler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journalfoeringsbehov;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.JournalpostMangler;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.KorrespendansePart;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoarkServiceTest {
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
    private ArgumentCaptor<FerdigstillJournalpostRequest> ferdigstillJournalpostCaptor;
    @Captor
    private ArgumentCaptor<OppdaterJournalpostRequest> oppdaterJournalpostRequestCaptor;
    @Captor
    private ArgumentCaptor<String> logiskVedleggTittelCaptor;

    @BeforeEach
    public void setUp() {
        this.joarkService = new JoarkService(inngaaendeJournalConsumer, journalConsumer, journalfoerInngaaendeConsumer, journalpostapiConsumer);
    }

    @Test
    void hentKjerneJournalpostListe() throws Exception {
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
    void konverterTilJournalfoeringmangler() {
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

        assertThat(journalfoeringMangler).isNotNull()
            .isNotEmpty()
            .contains(JournalfoeringMangel.ARKIVSAK)
            .doesNotContain(JournalfoeringMangel.AVSENDERID)
            .contains(JournalfoeringMangel.AVSENDERNAVN)
            .contains(JournalfoeringMangel.BRUKER)
            .doesNotContain(JournalfoeringMangel.FORSENDELSEINNSENDT)
            .contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI)
            .doesNotContain(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL)
            .contains(JournalfoeringMangel.INNHOLD)
            .contains(JournalfoeringMangel.TEMA);
    }

    @Test
    void oppdaterJournalpost_påkrevdeVerdierUtfylt() throws Exception {
        String tittel = "tittel";
        String hovedDokumentID = "1234";
        Map<String, String> vedleggMedTitler = new HashMap<>();
        String fysiskVedleggTittel = "Fysisk vedlegg";
        String fysiskVedleggID = "vedleggDokID";
        vedleggMedTitler.put(fysiskVedleggID, fysiskVedleggTittel);

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(1L)
            .medHovedDokumentID(hovedDokumentID).medBrukerID("12345")
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.ORGANISASJON)
            .medTittel(tittel).medFysiskeVedlegg(vedleggMedTitler)
            .medLogiskeVedleggTitler(Arrays.asList("dok1", "dok2")).medDokumentkategori(true).build();

        GetJournalpostResponse eksisterendeJournalpost = new GetJournalpostResponse();
        eksisterendeJournalpost.getDokumentListe().add(
            new Dokument().withDokumentId("dokID").withLogiskVedleggListe(
                List.of(new LogiskVedlegg().withLogiskVedleggTittel("tittel1").withLogiskVedleggId("id1"),
                    new LogiskVedlegg().withLogiskVedleggTittel("tittel2").withLogiskVedleggId("id2"))
            )
        );

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(eksisterendeJournalpost);
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, false);

        verify(journalpostapiConsumer, times(2)).fjernLogiskeVedlegg(anyString(), anyString());
        verify(journalpostapiConsumer).oppdaterJournalpost(oppdaterJournalpostRequestCaptor.capture(), anyString());
        OppdaterJournalpostRequest request = oppdaterJournalpostRequestCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.tittel).isEqualTo(tittel);
        assertThat(request.avsenderMottaker).isNotNull();
        assertThat(request.avsenderMottaker.getNavn()).isNotNull();
        assertThat(request.avsenderMottaker.getIdType()).isEqualTo(AvsenderMottaker.IdType.ORGNR);

        assertThat(request.bruker).isNotNull();
        assertThat(request.bruker.getId()).isNotNull();
        assertThat(request.bruker.getIdType()).isEqualTo(no.nav.melosys.integrasjon.joark.journalpostapi.dto.Bruker.BrukerIdType.FNR);

        assertThat(request.sak).isNotNull();
        assertThat(request.sak.getArkivsaksnummer()).isNotNull();
        assertThat(request.sak.getArkivsaksystem()).isNotNull();

        assertThat(request.dokumenter.size()).isEqualTo(2);
        assertThat(request.dokumenter.get(0).tittel).isEqualTo(tittel);
        assertThat(request.dokumenter.get(0).dokumentInfoId).isEqualTo(hovedDokumentID);
        assertThat(request.dokumenter.get(1).tittel).isEqualTo(fysiskVedleggTittel);
        assertThat(request.dokumenter.get(1).dokumentInfoId).isEqualTo(fysiskVedleggID);

        verify(journalpostapiConsumer, times(2)).leggTilLogiskVedlegg(anyString(), logiskVedleggTittelCaptor.capture());
        verify(journalpostapiConsumer, never()).ferdigstillJournalpost(any(), any());
        List<String> logiskVedleggRequest = logiskVedleggTittelCaptor.getAllValues();
        assertThat(logiskVedleggRequest.size()).isEqualTo(2);
        assertThat(logiskVedleggRequest.get(0)).isEqualTo("dok1");
        assertThat(logiskVedleggRequest.get(1)).isEqualTo("dok2");
    }

    @Test
    void oppdaterJournalpost_utenVedlegg_fungerer() throws Exception {
        String tittel = "tittel";
        String hovedDokumentID = "1234";
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(1L)
            .medBrukerID("12345").medHovedDokumentID(hovedDokumentID)
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.PERSON).medTittel(tittel).medFysiskeVedlegg(null)
            .medLogiskeVedleggTitler(null).medDokumentkategori(true).build();

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(new GetJournalpostResponse());
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, false);

        verify(journalpostapiConsumer, never()).fjernLogiskeVedlegg(any(), any());
        verify(journalpostapiConsumer).oppdaterJournalpost(oppdaterJournalpostRequestCaptor.capture(), anyString());
        OppdaterJournalpostRequest request = oppdaterJournalpostRequestCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.tittel).isEqualTo(tittel);
        assertThat(request.avsenderMottaker).isNotNull();
        assertThat(request.avsenderMottaker.getNavn()).isNotNull();

        assertThat(request.bruker).isNotNull();
        assertThat(request.bruker.getId()).isNotNull();
        assertThat(request.bruker.getIdType()).isNotNull();

        assertThat(request.sak).isNotNull();
        assertThat(request.sak.getArkivsaksnummer()).isNotNull();
        assertThat(request.sak.getArkivsaksystem()).isNotNull();

        assertThat(request.dokumenter).hasSize(1);
        Dokumentoppdatering hovedDokument = request.dokumenter.iterator().next();
        assertThat(hovedDokument.tittel).isEqualTo(tittel);
        assertThat(hovedDokument.dokumentInfoId).isEqualTo(hovedDokumentID);

        verify(journalpostapiConsumer, never()).leggTilLogiskVedlegg(anyString(), anyString());
    }

    @Test
    void oppdaterJournalpost_skalFerdigstilles_ferdigstillJournalpostBlirKalt() throws Exception {
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(1L)
            .medBrukerID("12345").build();

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(new GetJournalpostResponse());
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, true);

        verify(journalpostapiConsumer, never()).fjernLogiskeVedlegg(any(), any());
        verify(journalpostapiConsumer).oppdaterJournalpost(any(OppdaterJournalpostRequest.class), anyString());
        verify(journalpostapiConsumer).ferdigstillJournalpost(any(FerdigstillJournalpostRequest.class), eq("123"));
    }

    @Test
    void hentJournalpost_forventJournalpost() throws Exception {
        String arkivsakId = "123arkivsak";
        GetJournalpostResponse getJournalpostResponse = new GetJournalpostResponse();
        getJournalpostResponse.setJournalTilstand(GetJournalpostResponse.JournalTilstand.ENDELIG);
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
        String hovedDokLogiskVedlegg = "logisk vedlegg tittel";
        Dokument hoveddokument = new Dokument();
        hoveddokument.setTittel(dokumentTittel);
        hoveddokument.setDokumentId(dokumentId);
        hoveddokument.setNavSkjemaId(navSkjemaID);
        hoveddokument.getLogiskVedleggListe().add(new LogiskVedlegg().withLogiskVedleggTittel(hovedDokLogiskVedlegg));
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
        assertThat(journalpost.isErFerdigstilt()).isTrue();
        assertThat(journalpost.getForsendelseMottatt()).isEqualTo(forsendelseMottatt.toInstant());
        assertThat(journalpost.getHoveddokument().getDokumentId()).isEqualTo(dokumentId);
        assertThat(journalpost.getHoveddokument().getTittel()).isEqualTo(dokumentTittel);
        assertThat(journalpost.getHoveddokument().getNavSkjemaID()).isEqualTo(navSkjemaID);
        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg().size()).isEqualTo(1);
        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg().get(0).getTittel()).isEqualTo(hovedDokLogiskVedlegg);
        assertThat(journalpost.getArkivSakId()).isEqualTo(arkivsakId);
        assertThat(journalpost.getVedleggListe().size()).isEqualTo(2);
        assertThat(journalpost.getMottaksKanal()).isEqualTo(mottaksKanal);
    }

    @Test
    void hentMottaksDatoForJournalpost_journalpostFinnes_returnererMottaksdato() throws SikkerhetsbegrensningException, IntegrasjonException {
        final String journalpostID = "12421";
        GetJournalpostResponse response = new GetJournalpostResponse();
        response.getDokumentListe().add(new Dokument());
        response.setForsendelseMottatt(new Date());
        when(journalfoerInngaaendeConsumer.hentJournalpost(eq(journalpostID))).thenReturn(response);

        assertThat(joarkService.hentMottaksDatoForJournalpost(journalpostID))
            .isEqualTo(LocalDate.ofInstant(response.getForsendelseMottatt().toInstant(), ZoneId.systemDefault()));
    }

    @Test
    void ferdigstillJournalpost_journalpostBlirJournalført_ingenException() throws Exception {
        String journalpostId = "123";
        PutJournalpostResponse putJournalpostResponse = new PutJournalpostResponse();
        putJournalpostResponse.setHarEndeligJF(true);

        joarkService.ferdigstillJournalføring(journalpostId);

        verify(journalpostapiConsumer).ferdigstillJournalpost(ferdigstillJournalpostCaptor.capture(), eq(journalpostId));

        FerdigstillJournalpostRequest request = ferdigstillJournalpostCaptor.getValue();

        assertThat(request).isNotNull();
        assertThat(request.journalfoerendeEnhet).isEqualTo(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
    }

    @Test
    void opprettJournalpost_ikkeValider_forventMetodekall() throws FunksjonellException {
        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());

        String journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), false);

        verify(journalpostapiConsumer).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    @Test
    void opprettJournalpost_validerFelt_forventValidert() throws FunksjonellException {
        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());

        String journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), true);

        verify(journalpostapiConsumer).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    @Test
    void opprettJournalpost_validerFelt_forventException() {
        OpprettJournalpost opprettJournalpost = lagOpprettJournalpost();
        opprettJournalpost.setArkivSakId(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> joarkService.opprettJournalpost(opprettJournalpost, true));

        verify(journalpostapiConsumer, never()).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
    }

    @Test
    void opprettJournalpost_forsendelseMottattErSatt_forventDatoMottatt() throws FunksjonellException {
        OpprettJournalpost opprettJournalpost = lagOpprettJournalpost();
        opprettJournalpost.setForsendelseMottatt(Instant.now());

        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());
        joarkService.opprettJournalpost(opprettJournalpost, false);

        ArgumentCaptor<OpprettJournalpostRequest> captor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(journalpostapiConsumer).opprettJournalpost(captor.capture(), anyBoolean());

        OpprettJournalpostRequest opprettJournalpostRequest = captor.getValue();
        assertThat(opprettJournalpostRequest).isNotNull();
        assertThat(opprettJournalpostRequest.getDatoMottatt())
            .isEqualTo(LocalDate.ofInstant(opprettJournalpost.getForsendelseMottatt(), ZoneId.systemDefault()));
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