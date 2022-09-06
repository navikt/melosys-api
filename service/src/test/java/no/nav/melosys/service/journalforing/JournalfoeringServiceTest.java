package no.nav.melosys.service.journalforing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.BrukerIdType;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalfoeringServiceTest {

    private final String MELOSYS_SAKSNUMMER = "MEL-0123";
    private final String RINA_SAKSNUMMER = "22222";

    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private PersondataFasade persondataFasade;

    private final FakeUnleash unleash = new FakeUnleash();

    @Captor
    private ArgumentCaptor<Prosessinstans> prosessinstansArgumentCaptor;

    private JournalfoeringService journalfoeringService;
    private JournalfoeringOpprettDto opprettDto;
    private JournalfoeringTilordneDto tilordneDto;
    private Journalpost journalpost;
    private JournalfoeringSedDto journalfoeringSedDto;


    @BeforeEach
    public void setup() {
        unleash.enable("melosys.folketrygden.mvp");
        journalpost = new Journalpost("123");
        journalpost.setHoveddokument(new ArkivDokument());

        this.journalfoeringService = new JournalfoeringService(joarkFasade, prosessinstansService, eessiService, fagsakService, persondataFasade, unleash);
        opprettDto = new JournalfoeringOpprettDto();
        opprettDto.setJournalpostID("setJournalpostID");
        opprettDto.setOppgaveID("setOppgaveID");
        opprettDto.setAvsenderNavn("setAvsenderNavn");
        opprettDto.setAvsenderID("setAvsenderID");
        opprettDto.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        opprettDto.setBrukerID("setBrukerID");
        opprettDto.setHoveddokument(new DokumentDto("3333", "setDokumenttittel"));
        opprettDto.setArbeidsgiverID("123456789");
        opprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        opprettDto.setFagsak(new FagsakDto());

        tilordneDto = new JournalfoeringTilordneDto();
        tilordneDto.setBehandlingstypeKode(Behandlingstyper.ENDRET_PERIODE.getKode());
        tilordneDto.setJournalpostID("setJournalpostID");
        tilordneDto.setOppgaveID("setOppgaveID");
        tilordneDto.setAvsenderNavn("setAvsenderNavn");
        tilordneDto.setAvsenderID("setAvsenderID");
        tilordneDto.setAvsenderType(Avsendertyper.PERSON);
        tilordneDto.setBrukerID("setBrukerID");
        tilordneDto.setHoveddokument(new DokumentDto("123", "setDokumenttittel"));

        journalfoeringSedDto = new JournalfoeringSedDto();
        journalfoeringSedDto.setBrukerID("brukerID");
        journalfoeringSedDto.setJournalpostID("journalpostID");
        journalfoeringSedDto.setOppgaveID("321");
    }

    @Test
    void finnBrukerIdent_brukerIdentErFolkeregisterident_returnererIdent() {
        journalpost.setBrukerId("123");
        journalpost.setBrukerIdType(BrukerIdType.FOLKEREGISTERIDENT);
        assertThat(journalfoeringService.finnHovedpartIdent(journalpost)).isPresent().get().isEqualTo("123");
    }

    @Test
    void finnBrukerIdent_brukerIdentErAktørId_henterIdent() {
        final var ident = "123321";
        journalpost.setBrukerId("123");
        journalpost.setBrukerIdType(BrukerIdType.AKTØR_ID);
        when(persondataFasade.hentFolkeregisterident(journalpost.getBrukerId())).thenReturn(ident);
        assertThat(journalfoeringService.finnHovedpartIdent(journalpost)).isPresent().get().isEqualTo(ident);
    }

    @Test
    void finnBrukerIdent_brukerIdentErOrgnr_returnererOrgnr() {
        journalpost.setBrukerId("123");
        journalpost.setBrukerIdType(BrukerIdType.ORGNR);
        assertThat(journalfoeringService.finnHovedpartIdent(journalpost)).isPresent().get().isEqualTo("123");
    }

    @Test
    void finnBrukerIdent_brukerErNull_returnererIngenting() {
        assertThat(journalfoeringService.finnHovedpartIdent(journalpost)).isEmpty();
    }

    @Test
    void opprettSakOgJournalfør_ikkeSed_prosessinstansBlirOpprettet() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS);
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
    }

    @Test
    void opprettSakOgJournalfør_medVirksomhetOrgnr_oppretterKorrektProsessinstans() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS);
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBrukerID(null);
        opprettDto.setVirksomhetOrgnr("orgnr");
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_VIRKSOMHET), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSTYPE, Sakstyper.class)).isEqualTo(Sakstyper.EU_EOS);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class)).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
    }

    @Test
    void opprettSakOgJournalfør_sakstemaEnabled_oppretterKorrektProsessinstans() {
        unleash.enable("melosys.sakstema");
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS);
        fagsakDto.setSakstema(Sakstemaer.UNNTAK.getKode());
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstypeKode(Behandlingstyper.KLAGE.getKode());
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any())).thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSTEMA, Sakstemaer.class).getKode()).isEqualTo(fagsakDto.getSakstema());
        assertThat(lagretProsessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class).getKode()).isEqualTo(opprettDto.getBehandlingstypeKode());
    }

    @Test
    void opprettSakOgJournalfør_sakstypeFtrlUtenLandOgPeriode_prosessinstansBlirOpprettet() {
        FagsakDto fagsakDto = lagFagsakDto(null, null, null, Sakstyper.FTRL);
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstemaKode(Behandlingstema.ARBEID_I_UTLANDET.getKode());
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    @Test
    void opprettSakOgJournalfør_sakstypeFtrlBehandlingstemaArbeidFlereLand_feilKombinasjonSakstypeBehandlingstemaKasterFeil() {
        FagsakDto fagsakDto = lagFagsakDto(null, null, null, Sakstyper.FTRL);
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("ikke gyldig for sakstype");
    }

    @Test
    void opprettSakOgJournalfør_fomEtterTom_feiler() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MAX, LocalDate.MIN, "DK", Sakstyper.EU_EOS);
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("Fra og med dato kan ikke være etter til og med dato");
    }

    @Test
    void opprettSakOgJournalfør_utenTom_gyldig() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, null, "DK", Sakstyper.EU_EOS);
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    @Test
    void opprettSakOgJournalfør_sakstypeFtrlFeatureToggleFolketrygdMvpDisabled_kasterException() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, null, "DK", Sakstyper.FTRL);
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstemaKode(Behandlingstema.ARBEID_I_UTLANDET.getKode());
        unleash.disableAll();
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("Kan ikke opprette ny sak med behandlingstema " + Behandlingstema.ARBEID_I_UTLANDET);
    }

    @Test
    void opprettSakOgJournalfør_sakstypeFtrlFeatureToggleFolketrygdMvpEnabled_oppretterSak() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, null, "DK", Sakstyper.FTRL);
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstemaKode(Behandlingstema.ARBEID_I_UTLANDET.getKode());

        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);
        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    @Test
    void opprettSakOgJournalfør_oppgaveID_mangler() {
        opprettDto.setOppgaveID(null);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("OppgaveID mangler");
    }

    @Test
    void opprettOgJournalfør_støtterAutomatiskBehandling_forventException() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(RINA_SAKSNUMMER);
        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);

        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.TRUE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("skal ikke journalføres manuelt");
    }


    @Test
    void opprettOgJournalfør_støtterIkkeAutomatiskBehandling_feilBehandlingstype() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(RINA_SAKSNUMMER);
        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);

        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);
        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("Manuell journalføring");
    }

    @Test
    void opprettOgJournalfør_sedAlleredeTilknyttet_kasterException() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(RINA_SAKSNUMMER);
        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);

        final Long arkivsakID = 22244L;
        final Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-22");
        fagsak.setSaksnummer(arkivsakID.toString());
        opprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);
        when(eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER)).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    void opprettOgJournalfør_brukerIDOgVirksomheOrgnrMangler_kasterException() {
        opprettDto.setBrukerID(null);
        opprettDto.setVirksomhetOrgnr(null);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("Både BrukerID og VirksomhetOrgnr mangler. Krever én");
    }

    @Test
    void opprettOgJournalfør_brukerIDOgVirksomhetOrgnrFinnes_kasterException() {
        opprettDto.setBrukerID("fnr");
        opprettDto.setVirksomhetOrgnr("orgnr");
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("Både BrukerID og VirksomhetOrgnr finnes. Dette kan skape problemer. Velg én å journalføre dokumentet på.");
    }

    @Test
    void opprettOgJournalfør_journalpostErFerdigstilt_kasterException() {
        journalpost.setErFerdigstilt(true);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettSak(opprettDto))
            .withMessageContaining("allerede ferdigstilt");
    }

    @Test
    void journalførOgKnyttTilEksisterendeSak_altOK_prosessinstansOpprettet() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);
        when(prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, tilordneDto))
            .thenReturn(new Prosessinstans());

        journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getBehandling()).isEqualTo(behandling);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSNUMMER)).isEqualTo(MELOSYS_SAKSNUMMER);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.JFR_INGEN_VURDERING, Boolean.class))
            .isEqualTo(tilordneDto.isIngenVurdering());
    }

    @Test
    void journalførOgKnyttTilEksisterendeSak_behandlingstypeSED_prosessinstansOpprettet() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);
        tilordneDto.setBehandlingstypeKode(Behandlingstyper.SED.getKode());

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);
        when(prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, tilordneDto))
            .thenReturn(new Prosessinstans());

        journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getBehandling()).isEqualTo(behandling);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSNUMMER)).isEqualTo(MELOSYS_SAKSNUMMER);
        assertThat(lagretProsessinstans.getData(ProsessDataKey.JFR_INGEN_VURDERING, Boolean.class))
            .isEqualTo(tilordneDto.isIngenVurdering());
    }

    @Test
    void journalførOgKnyttTilEksisterendeSak_fagsakFinnesIkke_kasterFeil() {
        tilordneDto.setSaksnummer(null);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(null))
            .thenThrow(new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + null));

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto))
            .withMessageContaining("Det finnes ingen fagsak med saksnummer: null");
    }

    @Test
    void journalførOgKnyttTilEksisterendeSak_sedSakTilknyttetAnnenFagsak_kasterException() {
        journalpost.setMottaksKanal("EESSI");
        var melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(RINA_SAKSNUMMER);

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);
        fagsak.setSaksnummer("FAGSAK KOBLET TIL SED FRA FØR");

        tilordneDto.setSaksnummer("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG");

        Long arkivsakID = 111L;

        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);
        when(eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER)).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(fagsakService.hentFagsak("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG")).thenReturn(new Fagsak());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    void journalførOgKnyttTilEksisterendeSak_sistRegistrertBehandlingErAvsluttetOgSakHarBehandlingMedTypeSoeknad_kasterException() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);

        var behandlingSoeknad = new Behandling();
        behandlingSoeknad.setRegistrertDato(Instant.parse("2020-01-01T00:00:00Z"));
        behandlingSoeknad.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandlingSoeknad.setType(Behandlingstyper.SED);

        var behandlingSed = new Behandling();
        behandlingSed.setRegistrertDato(Instant.parse("2021-01-01T00:00:00Z"));
        behandlingSed.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingSed.setType(Behandlingstyper.SOEKNAD);

        var fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandlingSoeknad, behandlingSed));

        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto))
            .withMessage("Saker kun bestående av avsluttede behandlinger med f.eks behandlingstype SED har lov til å knytte til " +
                "eksisterende sak uten å opprette ny behandling. Denne saken inneholder en behandling med behandlingstype SOEKNAD.");
    }

    @Test
    void journalførOgOpprettNyVurdering_altOK_prosessinstansOpprettet() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);
        when(prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_VURDERING, tilordneDto))
            .thenReturn(new Prosessinstans());

        journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto);

        verify(prosessinstansService).lagre(prosessinstansArgumentCaptor.capture());
        var lagretProsessinstans = prosessinstansArgumentCaptor.getValue();
        assertThat(lagretProsessinstans.getBehandling()).isNull();
        assertThat(lagretProsessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class).getKode())
            .isEqualTo(tilordneDto.getBehandlingstypeKode());
        assertThat(lagretProsessinstans.getData(ProsessDataKey.SAKSNUMMER)).isEqualTo(tilordneDto.getSaksnummer());
        assertThat(lagretProsessinstans.getData(ProsessDataKey.JFR_INGEN_VURDERING, Boolean.class))
            .isEqualTo(tilordneDto.isIngenVurdering());
    }

    @Test
    void journalførOgOpprettNyVurdering_behandlingstypeIkkeTillattForSakstype_kasterException() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);
        var fagsak = new Fagsak();

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);


        tilordneDto.setBehandlingstypeKode(Behandlingstyper.ENDRET_PERIODE.getKode());
        fagsak.setType(Sakstyper.TRYGDEAVTALE);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto))
            .withMessageContaining(" er ikke en lovlig behandlingstype ved knytting av dokument til sak");


        tilordneDto.setBehandlingstypeKode(Behandlingstyper.KLAGE.getKode());
        fagsak.setType(Sakstyper.EU_EOS);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto))
            .withMessageContaining(" er ikke en lovlig behandlingstype ved knytting av dokument til sak");
    }

    @Test
    void journalførOgOpprettNyVurdering_fagsakHarAktivBehandling_feilKastes() {
        tilordneDto.setSaksnummer(MELOSYS_SAKSNUMMER);

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(MELOSYS_SAKSNUMMER)).thenReturn(fagsak);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto))
            .withMessageContaining("Det finnes allerede en aktiv behandling på fagsak " + MELOSYS_SAKSNUMMER);
    }

    @Test
    void journalførOgOpprettNyVurdering_sedSakTilknyttetAnnenFagsak_kasterException() {
        journalpost.setMottaksKanal("EESSI");
        var melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(RINA_SAKSNUMMER);

        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);
        fagsak.setSaksnummer("FAGSAK KOBLET TIL SED FRA FØR");

        tilordneDto.setSaksnummer("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG");

        Long arkivsakID = 111L;

        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);
        when(eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER)).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(fagsakService.hentFagsak("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG")).thenReturn(new Fagsak());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    void journalførOgOpprettNyVurdering_fagsakFinnesIkke_kasterFeil() {
        tilordneDto.setSaksnummer(null);

        when(joarkFasade.hentJournalpost(tilordneDto.getJournalpostID())).thenReturn(journalpost);
        when(fagsakService.hentFagsak(null))
            .thenThrow(new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + null));

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgOpprettNyVurdering(tilordneDto))
            .withMessageContaining("Det finnes ingen fagsak med saksnummer: null");
    }

    @Test
    void journalførSed_støtterIkkeAutomatiskBehandling_forventException() {
        when(eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.getJournalpostID())).thenReturn(false);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("støtter ikke automatisk behandling");
    }

    @Test
    void journalførSed_manglerBrukerID_forventException() {
        journalfoeringSedDto.setBrukerID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("BrukerID er påkrevd");
    }

    @Test
    void journalførSed_manglerJournalpostID_forventException() {
        journalfoeringSedDto.setJournalpostID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("JournalpostID er påkrevd");
    }

    @Test
    void journalførSed_manglerOppgaveID_forventException() {
        journalfoeringSedDto.setOppgaveID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("OppgaveID er påkrevd");
    }

    @Test
    void journalførSed_støtterAutomatiskBehandling_prosessinstansOpprettetOppgaveFerdigstilt() {
        final String aktørID = "432537";
        final MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setRinaSaksnummer("123");

        when(eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.getJournalpostID())).thenReturn(true);
        when(eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.getJournalpostID())).thenReturn(eessiMelding);
        when(persondataFasade.hentAktørIdForIdent(journalfoeringSedDto.getBrukerID())).thenReturn(aktørID);

        journalfoeringService.journalførSed(journalfoeringSedDto);
        verify(prosessinstansService).opprettProsessinstansSedMottak(eessiMelding, aktørID);
    }

    private FagsakDto lagFagsakDto(LocalDate fom, LocalDate tom, String land, Sakstyper sakstype) {
        FagsakDto fagsakDto = new FagsakDto();
        fagsakDto.setSakstype(sakstype.getKode());
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(fom);
        periode.setTom(tom);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(new SoeknadslandDto(Collections.singletonList(land), false));
        return fagsakDto;
    }
}
