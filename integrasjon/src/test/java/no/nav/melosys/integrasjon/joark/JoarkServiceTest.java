package no.nav.melosys.integrasjon.joark;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import no.finn.unleash.FakeUnleash;
import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.Dokument;
import no.nav.dok.tjenester.journalfoerinngaaende.LogiskVedlegg;
import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.arkiv.DokumentVariant;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.AvsenderMottaker;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import no.nav.melosys.integrasjon.joark.saf.SafConsumer;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*;
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
    private JournalConsumer journalConsumer;
    @Mock
    private JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;
    @Mock
    private JournalpostapiConsumer journalpostapiConsumer;
    @Mock
    private SafConsumer safConsumer;

    private final FakeUnleash unleash = new FakeUnleash();

    @Captor
    private ArgumentCaptor<FerdigstillJournalpostRequest> ferdigstillJournalpostCaptor;
    @Captor
    private ArgumentCaptor<OppdaterJournalpostRequest> oppdaterJournalpostRequestCaptor;
    @Captor
    private ArgumentCaptor<String> logiskVedleggTittelCaptor;

    @BeforeEach
    public void setUp() {
        this.joarkService = new JoarkService(journalConsumer, journalfoerInngaaendeConsumer, journalpostapiConsumer, safConsumer, unleash);
    }

    @Test
    void hentJournalposterTilknyttetSak_brukerGammelIntegrasjon_verifiserMapping() throws Exception {
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        final var fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(1L);
        fagsak.setSaksnummer("MEL-111");
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

        List<Journalpost> journalpostListe = joarkService.hentJournalposterTilknyttetSak(
            new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), fagsak.getSaksnummer())
        );

        assertThat(journalpostListe.size()).isEqualTo(1);

        Journalpost journalpost1 = journalpostListe.get(0);
        assertThat(journalpost1.getSaksnummer()).isEqualTo(fagsak.getSaksnummer());
        assertThat(journalpost1.getHoveddokument().getDokumentId()).isEqualTo(dokID);
        assertThat(journalpost1.getHoveddokument().getTittel()).isEqualTo(tittel);
        assertThat(journalpost1.getJournalposttype()).isEqualTo(Journalposttype.INN);
        assertThat(journalpost1.getForsendelseMottatt()).isNotNull();
        assertThat(journalpost1.getForsendelseJournalfoert()).isNotNull();
        assertThat(journalpost1.getKorrespondansepartId()).isEqualTo(partID);
        assertThat(journalpost1.getKorrespondansepartNavn()).isEqualTo(partNavn);
    }

    @Test
    void hentJournalposterTilknyttetSak_brukerSaf_mapperAlleSafJournalposter() throws SikkerhetsbegrensningException {
        unleash.enable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        final var saksnummer = "191919";
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost("111"), safJournalpost("222")));

        var journalposter = joarkService.hentJournalposterTilknyttetSak(new HentJournalposterTilknyttetSakRequest(null, saksnummer));
        assertThat(journalposter).hasSize(2);
    }


    @Test
    void oppdaterJournalpost_påkrevdeVerdierUtfylt() throws Exception {
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        String tittel = "tittel";
        String hovedDokumentID = "1234";
        Map<String, String> vedleggMedTitler = new HashMap<>();
        String fysiskVedleggTittel = "Fysisk vedlegg";
        String fysiskVedleggID = "vedleggDokID";
        vedleggMedTitler.put(fysiskVedleggID, fysiskVedleggTittel);

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-9")
            .medHovedDokumentID(hovedDokumentID).medBrukerID("12345")
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.ORGANISASJON)
            .medTittel(tittel).medFysiskeVedlegg(vedleggMedTitler)
            .medLogiskeVedleggTitler(Arrays.asList("dok1", "dok2")).build();

        GetJournalpostResponse eksisterendeJournalpost = new GetJournalpostResponse();
        eksisterendeJournalpost.setForsendelseMottatt(new Date());
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
        assertThat(request.sak.getFagsakId()).isEqualTo(journalpostOppdatering.getSaksnummer());
        assertThat(request.sak.getSakstype()).isEqualTo("FAGSAK");
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
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        String tittel = "tittel";
        String hovedDokumentID = "1234";
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-8")
            .medBrukerID("12345").medHovedDokumentID(hovedDokumentID)
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.PERSON).medTittel(tittel).medFysiskeVedlegg(null)
            .medLogiskeVedleggTitler(null).build();

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(
            new GetJournalpostResponse().withDokumentListe(List.of(new Dokument().withLogiskVedleggListe(List.of()))).withForsendelseMottatt(new Date())
        );
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
        assertThat(request.sak.getFagsakId()).isEqualTo(journalpostOppdatering.getSaksnummer());
        assertThat(request.sak.getSakstype()).isEqualTo("FAGSAK");
        assertThat(request.sak.getArkivsaksystem()).isNotNull();

        assertThat(request.dokumenter).hasSize(1);
        Dokumentoppdatering hovedDokument = request.dokumenter.iterator().next();
        assertThat(hovedDokument.tittel).isEqualTo(tittel);
        assertThat(hovedDokument.dokumentInfoId).isEqualTo(hovedDokumentID);

        verify(journalpostapiConsumer, never()).leggTilLogiskVedlegg(anyString(), anyString());
    }

    @Test
    void oppdaterJournalpost_skalFerdigstilles_ferdigstillJournalpostBlirKalt() throws Exception {
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-1111")
            .medBrukerID("12345").build();

        when(journalfoerInngaaendeConsumer.hentJournalpost(anyString())).thenReturn(
            new GetJournalpostResponse().withDokumentListe(List.of(new Dokument().withLogiskVedleggListe(List.of()))).withForsendelseMottatt(new Date())
        );
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, true);

        verify(journalpostapiConsumer, never()).fjernLogiskeVedlegg(any(), any());
        verify(journalpostapiConsumer).oppdaterJournalpost(any(OppdaterJournalpostRequest.class), anyString());
        verify(journalpostapiConsumer).ferdigstillJournalpost(any(FerdigstillJournalpostRequest.class), eq("123"));
    }

    @Test
    void hentJournalpost_forventJournalpost() throws Exception {
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

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
        assertThat(journalpost.getBrukerIdType()).isEqualTo(BrukerIdType.FOLKEREGISTERIDENT);
        assertThat(journalpost.getAvsenderId()).isEqualTo(avsenderId);
        assertThat(journalpost.isErFerdigstilt()).isTrue();
        assertThat(journalpost.getForsendelseMottatt()).isEqualTo(forsendelseMottatt.toInstant());
        assertThat(journalpost.getHoveddokument().getDokumentId()).isEqualTo(dokumentId);
        assertThat(journalpost.getHoveddokument().getTittel()).isEqualTo(dokumentTittel);
        assertThat(journalpost.getHoveddokument().getNavSkjemaID()).isEqualTo(navSkjemaID);
        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg().size()).isEqualTo(1);
        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg().get(0).getTittel()).isEqualTo(hovedDokLogiskVedlegg);
        //assertThat(journalpost.getArkivSakId()).isEqualTo(arkivsakId); -> arkivsakId fjernet, støtter ikke saksnummer til fagsak
        assertThat(journalpost.getVedleggListe().size()).isEqualTo(2);
        assertThat(journalpost.getMottaksKanal()).isEqualTo(mottaksKanal);
    }

    @Test
    void hentJournalpost_brukerSaf_verifiserMapping() throws FunksjonellException {
        unleash.enable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);
        final var journalpostID = "1112233";
        final var safJournalpost = safJournalpost(journalpostID);
        when(safConsumer.hentJournalpost(journalpostID)).thenReturn(safJournalpost);

        var journalpost = joarkService.hentJournalpost(journalpostID);

        assertThat(journalpost).extracting(
            Journalpost::getJournalpostId,
            Journalpost::getJournalposttype,
            Journalpost::getBrukerId,
            Journalpost::getBrukerIdType,
            Journalpost::getAvsenderId,
            Journalpost::getAvsenderNavn,
            Journalpost::getAvsenderType,
            Journalpost::getForsendelseJournalfoert,
            Journalpost::getForsendelseMottatt,
            Journalpost::getInnhold,
            Journalpost::getKorrespondansepartId,
            Journalpost::getKorrespondansepartNavn,
            Journalpost::getMottaksKanal,
            Journalpost::getTema
        ).containsExactly(
            safJournalpost.journalpostId(),
            Journalposttype.INN,
            safJournalpost.bruker().id(),
            BrukerIdType.FOLKEREGISTERIDENT,
            safJournalpost.avsenderMottaker().id(),
            safJournalpost.avsenderMottaker().navn(),
            Avsendertyper.ORGANISASJON,
            null,
            safJournalpost.relevanteDatoer().stream().filter(RelevantDato::harDatotypeRegistrert)
                .map(RelevantDato::dato).map(this::tilInstant).findFirst().orElseThrow(),
            safJournalpost.tittel(),
            safJournalpost.avsenderMottaker().id(),
            safJournalpost.avsenderMottaker().navn(),
            safJournalpost.kanal(),
            safJournalpost.tema()
        );

        final var safHovedDokument = safJournalpost.dokumenter().iterator().next();
        final var safLogiskVedlegg = safHovedDokument.logiskeVedlegg().get(0);
        assertThat(journalpost.getHoveddokument())
            .extracting(ArkivDokument::getDokumentId, ArkivDokument::getTittel, ArkivDokument::getNavSkjemaID)
            .containsExactly(safHovedDokument.dokumentInfoId(), safHovedDokument.tittel(), safHovedDokument.brevkode());

        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg())
            .flatExtracting(
                no.nav.melosys.domain.arkiv.LogiskVedlegg::getLogiskVedleggID,
                no.nav.melosys.domain.arkiv.LogiskVedlegg::getTittel)
            .containsExactly(
                safLogiskVedlegg.logiskVedleggId(),
                safLogiskVedlegg.tittel());
    }

    private Instant tilInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    void hentMottaksDatoForJournalpost_journalpostFinnes_returnererMottaksdato() throws FunksjonellException,
        IntegrasjonException {
        unleash.disable(JoarkService.SAF_FEATURE_TOGGLE_NAVN);

        final String journalpostID = "12421";
        GetJournalpostResponse response = new GetJournalpostResponse();
        response.getDokumentListe().add(new Dokument());
        response.setForsendelseMottatt(new Date());
        when(journalfoerInngaaendeConsumer.hentJournalpost(journalpostID)).thenReturn(response);

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
        opprettJournalpost.setSaksnummer(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> joarkService.opprettJournalpost(opprettJournalpost, true))
            .withMessageContaining("Saksnummer mangler");

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
        opprettJournalpost.setSaksnummer("MEL-111");
        opprettJournalpost.setBrukerId("12345678901");
        opprettJournalpost.setBrukerIdType(BrukerIdType.FOLKEREGISTERIDENT);
        opprettJournalpost.setKorrespondansepartNavn("navn");
        opprettJournalpost.setKorrespondansepartId("id");
        opprettJournalpost.setKorrespondansepartIdType("UTL_ORG");

        FysiskDokument hoveddokument = new FysiskDokument();
        hoveddokument.setTittel("tittel");
        hoveddokument.setBrevkode("brevkode");

        DokumentVariant dokumentVariant = DokumentVariant.lagDokumentVariant("dokument".getBytes());
        hoveddokument.setDokumentVarianter(Collections.singletonList(dokumentVariant));

        hoveddokument.setDokumentKategori(DokumentKategoriKode.SED.name());
        opprettJournalpost.setHoveddokument(hoveddokument);

        return opprettJournalpost;
    }

    private no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost safJournalpost(String journalpostID) {
        var logiskVedlegg = new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.LogiskVedlegg("4143", "Tittel logisk vedlegg");
        return new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost(
            journalpostID,
            "Tittel",
            Journalstatus.MOTTATT,
            Tema.MED.getKode(),
            no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalposttype.I,
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Sak("MEL-123"),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Bruker("123123", Brukertype.FNR),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS"),
            "SKAN_NETS",
            Set.of(
                new RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)
            ),
            List.of(
                new DokumentInfo("123", "hoveddokument kommer først", null, List.of(logiskVedlegg)),
                new DokumentInfo("123", "vedlegg kommer etterpå", null, List.of())
            )
        );
    }
}
