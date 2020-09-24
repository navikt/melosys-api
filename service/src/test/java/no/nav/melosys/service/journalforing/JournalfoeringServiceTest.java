package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalfoeringServiceTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private TpsFasade tpsFasade;

    private JournalfoeringService journalfoeringService;
    private JournalfoeringOpprettDto opprettDto;
    private JournalfoeringTilordneDto tilordneDto;
    private Journalpost journalpost;
    private JournalfoeringSedDto journalfoeringSedDto;

    private final String rinaSaksnummer = "22222";

    @Before
    public void setup() throws MelosysException {

        journalpost = new Journalpost("123");
        journalpost.setHoveddokument(new ArkivDokument());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        this.journalfoeringService = new JournalfoeringService(joarkFasade, oppgaveService, prosessinstansService, eessiService, fagsakService, tpsFasade);
        opprettDto = new JournalfoeringOpprettDto();
        opprettDto.setJournalpostID("setJournalpostID");
        opprettDto.setOppgaveID("setOppgaveID");
        opprettDto.setAvsenderNavn("setAvsenderNavn");
        opprettDto.setAvsenderID("setAvsenderID");
        opprettDto.setAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        opprettDto.setBrukerID("setBrukerID");
        opprettDto.setHoveddokument(new DokumentDto("3333","setDokumenttittel"));
        opprettDto.setArbeidsgiverID("123456789");
        opprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

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

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer(rinaSaksnummer);
        when(eessiService.hentSedTilknyttetJournalpost(eq(journalpost.getJournalpostId()))).thenReturn(melosysEessiMelding);
    }

    @Test
    public void opprettSakOgJournalfør_ikkeSed_prosessinstansBlirOpprettet() throws MelosysException {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK");
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK), any()))
            .thenReturn(new Prosessinstans());

        journalfoeringService.opprettOgJournalfør(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());
    }

    @Test
    public void opprettSakOgJournalfør_fomEtterTom_feiler() {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MAX, LocalDate.MIN, "DK");
        opprettDto.setFagsak(fagsakDto);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("Fra og med dato kan ikke være etter til og med dato");
    }

    @Test
    public void opprettSakOgJournalfør_utenTom_gyldig() throws MelosysException {
        FagsakDto fagsakDto = lagFagsakDto(LocalDate.MIN, null, "DK");
        opprettDto.setFagsak(fagsakDto);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_SAK), any()))
            .thenReturn(new Prosessinstans());

        journalfoeringService.opprettOgJournalfør(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());
    }

    @Test
    public void opprettSakOgJournalfør_oppgaveID_mangler() {
        opprettDto.setOppgaveID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("OppgaveID mangler");
    }

    @Test
    public void opprettOgJournalfør_støtterAutomatiskBehandling_forventException() {
        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.TRUE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("skal ikke journalføres manuelt");
    }

    @Test
    public void opprettOgJournalfør_støtterIkkeAutomatiskBehandling_korrektBehandlingstype() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.TRYGDETID.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansGenerellSedBehandling(any(JournalfoeringOpprettDto.class));
    }

    @Test
    public void opprettOgJournalfør_støtterIkkeAutomatiskBehandling_feilBehandlingstype() {
        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("Opprettelse av behandling med tema");
    }

    @Test
    public void opprettOgJournalfør_sedAlleredeTilknyttet_kasterException() throws MelosysException {
        final Long arkivsakID = 22244L;
        final Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-22");
        fagsak.setSaksnummer(arkivsakID.toString());
        opprettDto.setBehandlingstemaKode(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(any(MelosysEessiMelding.class))).thenReturn(Boolean.FALSE);
        when(eessiService.finnSakForRinasaksnummer(eq(rinaSaksnummer))).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(eq(arkivsakID))).thenReturn(Optional.of(fagsak));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    public void opprettOgJournalfør_erVurderTrygdetid_prosessinstansOpprettet() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.TRYGDETID.getKode());
        journalpost.setMottaksKanal("EESSI");

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansGenerellSedBehandling(opprettDto);
    }

    @Test
    public void opprettOgJournalfør_brukerIDMangler_kasterException() {
        opprettDto.setBrukerID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("BrukerID mangler");
    }


    @Test
    public void tilordneSakOgJournalfør_ønskerNyBehandlingEndretPeriode_ingenAktiveBehandlingerProsessinstansOpprettet() throws MelosysException {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_NY_BEHANDLING), any()))
            .thenReturn(new Prosessinstans());
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());
    }

    @Test
    public void tilordneSakOgJournalfør_ønskerNyBehandlingEndretPeriode_finnesEnAktivBehandlingKasterException() throws MelosysException {
        final String saksnummer = "MEL-0123";
        tilordneDto.setSaksnummer(saksnummer);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.tilordneSakOgJournalfør(tilordneDto))
            .withMessageContaining("Det finnes allerede en aktiv behandling på fagsak");
    }

    @Test
    public void tilordneSakOgJournalfør_saksnr_mangler() {
        tilordneDto.setSaksnummer("");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.tilordneSakOgJournalfør(tilordneDto))
            .withMessageContaining("Saksnummer mangler");
    }

    @Test
    public void tilordneSakOgJournalfør_sakTilknyttetAnnenFagsak_kasterException() throws MelosysException {
        final Long arkivsakID = 432L;
        final Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-0");

        tilordneDto.setSaksnummer("MEL-555");
        journalpost.setMottaksKanal("EESSI");

        when(eessiService.finnSakForRinasaksnummer(eq(rinaSaksnummer))).thenReturn(Optional.of(arkivsakID));
        when(fagsakService.finnFagsakFraArkivsakID(eq(arkivsakID))).thenReturn(Optional.of(fagsak));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.tilordneSakOgJournalfør(tilordneDto))
            .withMessageContaining("er allerede tilknyttet");
    }

    @Test
    public void journalførSed_støtterIkkeAutomatiskBehandling_forventException() throws MelosysException {
        when(eessiService.støtterAutomatiskBehandling(eq(journalfoeringSedDto.getJournalpostID()))).thenReturn(false);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("støtter ikke automatisk behandling");
    }

    @Test
    public void journalførSed_manglerBrukerID_forventException() {
        journalfoeringSedDto.setBrukerID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("BrukerID er påkrevd");
    }

    @Test
    public void journalførSed_manglerJournalpostID_forventException() {
        journalfoeringSedDto.setJournalpostID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("JournalpostID er påkrevd");
    }

    @Test
    public void journalførSed_manglerOppgaveID_forventException() {
        journalfoeringSedDto.setOppgaveID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("OppgaveID er påkrevd");
    }

    @Test
    public void journalførSed_støtterAutomatiskBehandling_prosessinstansOpprettetOppgaveFerdigstilt() throws MelosysException {
        final String aktørID = "432537";
        final MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setRinaSaksnummer("123");

        when(eessiService.støtterAutomatiskBehandling(eq(journalfoeringSedDto.getJournalpostID()))).thenReturn(true);
        when(eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.getJournalpostID())).thenReturn(eessiMelding);
        when(tpsFasade.hentAktørIdForIdent(eq(journalfoeringSedDto.getBrukerID()))).thenReturn(aktørID);

        journalfoeringService.journalførSed(journalfoeringSedDto);
        verify(prosessinstansService).opprettProsessinstansSedMottak(eq(eessiMelding), eq(aktørID));
    }

    private FagsakDto lagFagsakDto(LocalDate fom, LocalDate tom, String land) {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(fom);
        periode.setTom(tom);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Collections.singletonList("DK"));
        return fagsakDto;
    }
}