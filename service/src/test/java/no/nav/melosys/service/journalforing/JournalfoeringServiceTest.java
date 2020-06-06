package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
import static org.assertj.core.api.Assertions.assertThat;
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

    private JournalfoeringService journalfoeringService;
    private JournalfoeringOpprettDto opprettDto;
    private JournalfoeringTilordneDto tilordneDto;
    private Journalpost journalpost;
    private JournalfoeringSedDto journalfoeringSedDto;

    @Before
    public void setup() throws SikkerhetsbegrensningException, IntegrasjonException {

        journalpost = new Journalpost("123");
        journalpost.setHoveddokument(new ArkivDokument());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        this.journalfoeringService = new JournalfoeringService(joarkFasade, oppgaveService, prosessinstansService, eessiService);
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
    public void opprettOgJournalfør_støtterAutomatiskBehandling_forventException() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(anyString())).thenReturn(Boolean.TRUE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("skal ikke journalføres manuelt");
    }

    @Test
    public void opprettOgJournalfør_støtterIkkeAutomatiskBehandling_korrektBehandlingstype() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.TRYGDETID.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(anyString())).thenReturn(Boolean.FALSE);

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansGenerellSedBehandling(any(JournalfoeringOpprettDto.class));
    }

    @Test
    public void opprettOgJournalfør_støtterIkkeAutomatiskBehandling_feilBehandlingstype() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.getKode());
        journalpost.setMottaksKanal("EESSI");
        when(eessiService.støtterAutomatiskBehandling(anyString())).thenReturn(Boolean.FALSE);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.opprettOgJournalfør(opprettDto))
            .withMessageContaining("Manuell journalføring av behandlingstema");
    }

    @Test
    public void opprettOgJournalfør_erAnmodningUnntakHovedregel_prosessinstansOpprettet() throws MelosysException {
        AnmodningOmUnntakDto anmodningOmUnntakDto = new AnmodningOmUnntakDto();
        anmodningOmUnntakDto.setLovvalgsbestemmelse(FO_883_2004_ART16_1.getKode());
        anmodningOmUnntakDto.setUnntakFraLovvalgsbestemmelse(FO_883_2004_ART12_1.getKode());
        anmodningOmUnntakDto.setUnntakFraLovvalgsland("DE");
        opprettDto.setAnmodningOmUnntak(anmodningOmUnntakDto);

        FagsakDto fagsakDto = lagFagsakDto(LocalDate.now(), LocalDate.now().plusYears(1), "NO");
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstemaKode(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode());

        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_AOU_BREV), any()))
            .thenReturn(new Prosessinstans());
        journalfoeringService.opprettOgJournalfør(opprettDto);

        ArgumentCaptor<Prosessinstans> captor = ArgumentCaptor.forClass(Prosessinstans.class);
        verify(prosessinstansService).lagre(captor.capture());

        Prosessinstans prosessinstans = captor.getValue();
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING);
    }

    @Test
    public void opprettOgJournalfør_erVurderTrygdetid_prosessinstansOpprettet() throws MelosysException {
        opprettDto.setBehandlingstemaKode(Behandlingstema.TRYGDETID.getKode());
        journalpost.setMottaksKanal("EESSI");

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansGenerellSedBehandling(opprettDto);
    }

    @Test
    public void tilordneSakOgJournalfør() throws FunksjonellException, TekniskException {
        tilordneDto.setSaksnummer("MEL-0123");
        when(prosessinstansService.lagJournalføringProsessinstans(eq(ProsessType.JFR_KNYTT), any()))
            .thenReturn(new Prosessinstans());
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void tilordneSakOgJournalfør_saksnr_mangler() {
        tilordneDto.setSaksnummer("");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.tilordneSakOgJournalfør(tilordneDto))
            .withMessageContaining("Saksnummer mangler");
    }

    @Test
    public void valider() throws FunksjonellException {
        journalfoeringService.valider(opprettDto);
    }

    @Test
    public void valider_brukerID_mangler() {
        opprettDto.setBrukerID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.valider(opprettDto))
            .withMessageContaining("BrukerID mangler");
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
            .withMessageContaining("BrukerID mangler");
    }

    @Test
    public void journalførSed_manglerJournalpostID_forventException() {
        journalfoeringSedDto.setJournalpostID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("JournalpostID mangler");
    }

    @Test
    public void journalførSed_manglerOppgaveID_forventException() {
        journalfoeringSedDto.setOppgaveID(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> journalfoeringService.journalførSed(journalfoeringSedDto))
            .withMessageContaining("OppgaveID mangler");
    }

    @Test
    public void journalførSed_støtterAutomatiskBehandling_prosessinstansOpprettetOppgaveFerdigstilt() throws MelosysException {
        when(eessiService.støtterAutomatiskBehandling(eq(journalfoeringSedDto.getJournalpostID()))).thenReturn(true);
        journalfoeringService.journalførSed(journalfoeringSedDto);
        verify(prosessinstansService).opprettProsessinstansSedMottak(eq(journalfoeringSedDto.getJournalpostID()), eq(journalfoeringSedDto.getBrukerID()));
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