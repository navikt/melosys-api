package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.ArrayList;
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
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalfoeringServiceTest {
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

    private JournalfoeringService journalfoeringService;
    private JournalfoeringOpprettDto opprettDto;
    private JournalfoeringTilordneDto tilordneDto;
    private Journalpost journalpost;
    private JournalfoeringSedDto journalfoeringSedDto;

    private final String rinaSaksnummer = "22222";

    @BeforeEach
    public void setup() {
        unleash.enableAll();
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
        assertThat(journalfoeringService.finnBrukerIdent(journalpost)).contains(journalpost.getBrukerId());
    }

    @Test
    void finnBrukerIdent_brukerIdentErAktørId_henterIdent() {
        final var ident = "123321";
        journalpost.setBrukerId("123");
        journalpost.setBrukerIdType(BrukerIdType.AKTØR_ID);
        when(persondataFasade.hentFolkeregisterident(journalpost.getBrukerId())).thenReturn(ident);
        assertThat(journalfoeringService.finnBrukerIdent(journalpost)).contains(ident);
    }

    @Test
    void finnBrukerIdent_brukerIdentErOrgnr_returnererIngenting() {
        journalpost.setBrukerId("123");
        journalpost.setBrukerIdType(BrukerIdType.ORGNR);
        assertThat(journalfoeringService.finnBrukerIdent(journalpost)).isEmpty();
    }

    @Test
    void finnBrukerIdent_brukerErNull_returnererIngenting() {
        assertThat(journalfoeringService.finnBrukerIdent(journalpost)).isEmpty();
    }

    @Test
    void opprettSakOgJournalfør_ikkeSed_prosessinstansBlirOpprettet() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS);
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK_BRUKER), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgOpprettSak(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
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

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
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
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
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
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
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
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);

        final Long arkivsakID = 22244L;
        final Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-22");
        fagsak.setSaksnummer(arkivsakID.toString());
        opprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);
        when(eessiService.finnSakForRinasaksnummer(rinaSaksnummer)).thenReturn(Optional.of(arkivsakID));
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
    void tilordneSakOgJournalfør_ønskerNyBehandlingEndretPeriode_ingenAktiveBehandlingerProsessinstansOpprettet() {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_VURDERING), any()))
            .thenReturn(new Prosessinstans());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        journalfoeringService.journalførOgTilordneSak(tilordneDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    @Test
    void tilordneSakOgJournalfør_ønskerNyBehandlingEndretPeriode_finnesEnAktivBehandlingKasterException() {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining("Det finnes allerede en aktiv behandling på fagsak");
    }

    @Test
    void tilordneSakOgJournalfør_saksnr_mangler() {
        tilordneDto.setSaksnummer("");
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining("Saksnummer mangler");
    }

    @Test
    void tilordneSakOgJournalfør_sakTilknyttetAnnenFagsak_kasterException() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
        when(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId())).thenReturn(melosysEessiMelding);

        final Long arkivsakID = 432L;
        final Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-0");

        tilordneDto.setSaksnummer("MEL-555");
        journalpost.setMottaksKanal("EESSI");

        when(eessiService.finnSakForRinasaksnummer(rinaSaksnummer)).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    void tilordneSakOgJournalfør_behandlingstypeIkkeTillattForSakstype_kasterException() {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        tilordneDto.setBehandlingstypeKode(Behandlingstyper.ENDRET_PERIODE.getKode());
        fagsak.setType(Sakstyper.TRYGDEAVTALE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining(" er ikke en lovlig behandlingstype ved knytting av dokument til sak");

        tilordneDto.setBehandlingstypeKode(Behandlingstyper.KLAGE.getKode());
        fagsak.setType(Sakstyper.EU_EOS);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining(" er ikke en lovlig behandlingstype ved knytting av dokument til sak");
    }

    @Test
    void tilordneSakOgJournalfør_avsluttetetBehandlingErIkkeTillattVedKnyttingAvDokument_kasterException() {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        fagsak.setType(Sakstyper.TRYGDEAVTALE);
        tilordneDto.setBehandlingstypeKode(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining("sisteBehandling (ID:1) for Fagsak MEL-0123 er avsluttet");

        fagsak.setType(Sakstyper.EU_EOS);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførOgTilordneSak(tilordneDto))
            .withMessageContaining("sisteBehandling (ID:1) for Fagsak MEL-0123 er avsluttet");
    }

    @Test
    void tilordneSakOgJournalfør_knyttVedAktiveBehandling_prosessinstansOpprettet() {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);
        Prosessinstans value = new Prosessinstans();
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_KNYTT), any()))
            .thenReturn(value);

        tilordneDto.setBehandlingstypeKode(null);
        journalfoeringService.journalførOgTilordneSak(tilordneDto);
        verify(prosessinstansService).lagre(value);

        assertThat(value.getBehandling().getId()).isEqualTo(behandling.getId());
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
