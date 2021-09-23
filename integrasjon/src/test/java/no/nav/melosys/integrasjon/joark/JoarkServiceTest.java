package no.nav.melosys.integrasjon.joark;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.arkiv.DokumentVariant;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.AvsenderMottaker;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import no.nav.melosys.integrasjon.joark.saf.SafConsumer;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoarkServiceTest {
    private JoarkService joarkService;

    @Mock
    private JournalpostapiConsumer journalpostapiConsumer;
    @Mock
    private SafConsumer safConsumer;

    @Captor
    private ArgumentCaptor<FerdigstillJournalpostRequest> ferdigstillJournalpostCaptor;
    @Captor
    private ArgumentCaptor<OppdaterJournalpostRequest> oppdaterJournalpostRequestCaptor;
    @Captor
    private ArgumentCaptor<String> logiskVedleggTittelCaptor;

    private static final String VEDLEGG_MED_TILGANG_ID = "124";
    private static final String VEDLEGG_UTEN_TILGANG_ID = "125";

    @BeforeEach
    public void setUp() {
        this.joarkService = new JoarkService(journalpostapiConsumer, safConsumer);
    }

    @Test
    void hentJournalposterTilknyttetSak_brukerSaf_mapperAlleSafJournalposter() {
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost("111"), safJournalpost("222")));

        var journalposter = joarkService.hentJournalposterTilknyttetSak(new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer));
        assertThat(journalposter).hasSize(2);
    }


    @Test
    void oppdaterJournalpost_påkrevdeVerdierUtfylt() {
        final String journalpostID = "11112233";
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

        when(safConsumer.hentJournalpost(anyString())).thenReturn(safJournalpost(journalpostID));
        joarkService.oppdaterJournalpost("123", journalpostOppdatering, false);

        verify(journalpostapiConsumer).fjernLogiskeVedlegg(anyString(), anyString());
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
        assertThat(request.sak.getFagsaksystem()).isNotNull();

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
    void oppdaterJournalpost_utenVedlegg_fungerer() {

        String tittel = "tittel";
        String hovedDokumentID = "1234";
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-8")
            .medBrukerID("12345").medHovedDokumentID(hovedDokumentID)
            .medAvsenderID("12").medAvsenderNavn("321").medAvsenderType(Avsendertyper.PERSON).medTittel(tittel).medFysiskeVedlegg(null)
            .medLogiskeVedleggTitler(null).build();

        var safJournalpost = safJournalpost("123", false);

        when(safConsumer.hentJournalpost(anyString())).thenReturn(safJournalpost);
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
        assertThat(request.sak.getFagsaksystem()).isNotNull();

        assertThat(request.dokumenter).hasSize(1);
        Dokumentoppdatering hovedDokument = request.dokumenter.iterator().next();
        assertThat(hovedDokument.tittel).isEqualTo(tittel);
        assertThat(hovedDokument.dokumentInfoId).isEqualTo(hovedDokumentID);

        verify(journalpostapiConsumer, never()).leggTilLogiskVedlegg(anyString(), anyString());
    }

    @Test
    void oppdaterJournalpost_skalFerdigstilles_ferdigstillJournalpostBlirKalt() {

        final var journalpostID = "123321";
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-1111")
            .medBrukerID("12345").build();

        when(safConsumer.hentJournalpost(anyString())).thenReturn(safJournalpost(journalpostID, false));
        joarkService.oppdaterJournalpost(journalpostID, journalpostOppdatering, true);

        verify(journalpostapiConsumer, never()).fjernLogiskeVedlegg(any(), any());
        verify(journalpostapiConsumer).oppdaterJournalpost(any(OppdaterJournalpostRequest.class), anyString());
        verify(journalpostapiConsumer).ferdigstillJournalpost(any(FerdigstillJournalpostRequest.class), eq(journalpostID));
    }

    @Test
    void validerTilgangTilArkivVariant_harTilgangTilVedlegg_kasterIngenException() {
        final var journalpostId = "11122233";
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;
        final var safJournalpost = safJournalpost(journalpostId);
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost));

        Collection<DokumentReferanse> dokumentReferanser = Collections.singletonList(new DokumentReferanse(journalpostId, VEDLEGG_MED_TILGANG_ID));

        assertThatNoException()
            .isThrownBy(() -> joarkService.validerDokumenterTilhørerSakOgHarTilgang(
                new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer),
                dokumentReferanser));
    }

    @Test
    void validerTilgangTilArkivVariant_dokumentReferanserCollectionErTom_henterIkkeDokumentoversikt() {
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;

        Collection<DokumentReferanse> dokumentReferanser = Collections.emptyList();

        joarkService.validerDokumenterTilhørerSakOgHarTilgang(
            new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer),
            dokumentReferanser);

        verify(safConsumer, never()).hentDokumentoversikt(anyString());
    }

    @Test
    void validerTilgangTilArkivVariant_harikkeTilgangTilVedlegg_kasterSikkerhetsbegrensningException() {
        final var journalpostId = "11122233";
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;
        final var safJournalpost = safJournalpost(journalpostId);
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost));

        Collection<DokumentReferanse> dokumentReferanser = Collections.singletonList(new DokumentReferanse(journalpostId, VEDLEGG_UTEN_TILGANG_ID));
        final var request = new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser))
            .withMessageContaining("Ikke tilgang");
    }

    @Test
    void validerTilgangTilArkivVariant_journalPosterIkkeTilknyttetSak_kasterFunksjonellException() {
        final var journalpostId = "11122233";
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;
        final var safJournalpost = safJournalpost(journalpostId);
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost));

        Collection<DokumentReferanse> dokumentReferanser = Collections.singletonList(new DokumentReferanse("12345", VEDLEGG_MED_TILGANG_ID));
        final var request = new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser))
            .withMessageContaining("tilhører ikke sak ");

    }

    @Test
    void validerTilgangTilArkivVariant_feilJournalPostID_kasterIkkeFunnetException() {
        final var journalpostId = "11122233";
        final var saksnummer = "191919";
        final var arkivsakID = 12345L;
        final var safJournalpost = safJournalpostUtenVedlegg(journalpostId);
        when(safConsumer.hentDokumentoversikt(saksnummer)).thenReturn(List.of(safJournalpost));

        Collection<DokumentReferanse> dokumentReferanser = Collections.singletonList(new DokumentReferanse(journalpostId, VEDLEGG_MED_TILGANG_ID));
        final var request = new HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser))
            .withMessageContaining("Finner ikke dokument ");

    }

    @Test
    void hentJournalpost_verifiserMapping() {
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
            Journalpost::getAvsenderLand,
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
            "FINLAND",
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
        final var safDokumentVariant = safHovedDokument.dokumentvarianter().get(0);
        assertThat(journalpost.getHoveddokument())
            .extracting(ArkivDokument::getDokumentId, ArkivDokument::getTittel, ArkivDokument::getNavSkjemaID)
            .containsExactly(safHovedDokument.dokumentInfoId(), safHovedDokument.tittel(), safHovedDokument.brevkode());

        assertThat(journalpost.getHoveddokument().getLogiskeVedlegg())
            .flatExtracting(
                no.nav.melosys.domain.arkiv.LogiskVedlegg::logiskVedleggID,
                no.nav.melosys.domain.arkiv.LogiskVedlegg::tittel)
            .containsExactly(
                safLogiskVedlegg.logiskVedleggId(),
                safLogiskVedlegg.tittel());

        assertThat(journalpost.getHoveddokument().getDokumentVarianter())
            .flatExtracting(
                DokumentVariant::getSaksbehandlerHarTilgang,
                dokumentVariant -> dokumentVariant.getVariantFormat().name())
            .containsExactly(
                safDokumentVariant.saksbehandlerHarTilgang(),
                safDokumentVariant.variantformat());
    }

    private Instant tilInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    void hentMottaksDatoForJournalpost_journalpostFinnes_returnererMottaksdato() {

        final String journalpostID = "12421";
        final var safJournalpost = safJournalpost(journalpostID);
        final var forventetMottaksdato = safJournalpost.relevanteDatoer().stream()
            .filter(RelevantDato::harDatotypeRegistrert)
            .map(RelevantDato::dato)
            .map(LocalDateTime::toLocalDate)
            .findFirst()
            .orElseThrow();

        when(safConsumer.hentJournalpost(journalpostID)).thenReturn(safJournalpost);

        assertThat(joarkService.hentMottaksDatoForJournalpost(journalpostID)).isNotNull().isEqualTo(forventetMottaksdato);
    }

    @Test
    void ferdigstillJournalpost_journalpostBlirJournalført_ingenException() {
        String journalpostId = "123";
        joarkService.ferdigstillJournalføring(journalpostId);

        verify(journalpostapiConsumer).ferdigstillJournalpost(ferdigstillJournalpostCaptor.capture(), eq(journalpostId));

        assertThat(ferdigstillJournalpostCaptor.getValue())
            .extracting(f -> f.journalfoerendeEnhet)
            .isEqualTo(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
    }

    @Test
    void opprettJournalpost_ikkeValider_forventMetodekall() {
        when(journalpostapiConsumer.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").build());

        String journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), false);

        verify(journalpostapiConsumer).opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean());
        assertThat(journalpostId).isNotEmpty();
    }

    @Test
    void opprettJournalpost_validerFelt_forventValidert() {
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
    void opprettJournalpost_forsendelseMottattErSatt_forventDatoMottatt() {
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
        return safJournalpost(journalpostID, true);
    }

    private no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost safJournalpost(String journalpostID, boolean medLogiskVedlegg) {
        var logiskVedlegg = new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.LogiskVedlegg("4143", "Tittel logisk vedlegg");
        var dokumentVedlegg = new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.DokumentVariant(true, Variantformat.ARKIV.name());
        return new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost(
            journalpostID,
            "Tittel",
            Journalstatus.MOTTATT,
            Tema.MED.getKode(),
            no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalposttype.I,
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Sak("MEL-123"),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Bruker("123123", Brukertype.FNR),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS", "FINLAND"),
            "SKAN_NETS",
            Set.of(
                new RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)
            ),
            List.of(
                new DokumentInfo("123", "hoveddokument kommer først", null, medLogiskVedlegg ? List.of(logiskVedlegg) : List.of(), List.of(dokumentVedlegg)),
                new DokumentInfo(VEDLEGG_MED_TILGANG_ID, "vedlegg kommer etterpå", null, List.of(), List.of(
                    new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.DokumentVariant(
                        true,
                        Variantformat.ARKIV.name())
                )),
                new DokumentInfo(VEDLEGG_UTEN_TILGANG_ID, "tredje dokument", null, List.of(), List.of(
                    new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.DokumentVariant(
                        false,
                        Variantformat.ARKIV.name())
                ))
            )
        );
    }

    private no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost safJournalpostUtenVedlegg(String journalpostID) {
        return new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost(
            journalpostID,
            "Tittel",
            Journalstatus.MOTTATT,
            Tema.MED.getKode(),
            no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalposttype.I,
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Sak("MEL-123"),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Bruker("123123", Brukertype.FNR),
            new no.nav.melosys.integrasjon.joark.saf.dto.journalpost.AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS", null),
            "SKAN_NETS",
            Set.of(
                new RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)
            ),
            List.of(new DokumentInfo("123", "hoveddokument kommer først", null, List.of(), List.of())
            )
        );
    }


}
