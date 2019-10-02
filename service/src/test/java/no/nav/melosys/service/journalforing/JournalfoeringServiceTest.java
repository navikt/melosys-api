package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Arrays;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.FagsakDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Before
    public void setup() throws SikkerhetsbegrensningException, IntegrasjonException {

        journalpost = new Journalpost("123");
        journalpost.setHoveddokument(new ArkivDokument());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        this.journalfoeringService = new JournalfoeringService(joarkFasade, oppgaveService, prosessinstansService, eessiService);
        JournalfoeringOpprettDto opprettDto = new JournalfoeringOpprettDto();
        opprettDto.setJournalpostID("setJournalpostID");
        opprettDto.setDokumentID("setDokumentID");
        opprettDto.setOppgaveID("setOppgaveID");
        opprettDto.setAvsenderNavn("setAvsenderNavn");
        opprettDto.setAvsenderID("setAvsenderID");
        opprettDto.setBrukerID("setBrukerID");
        opprettDto.setHoveddokumentTittel("setDokumenttittel");
        opprettDto.setArbeidsgiverID("123456789");
        this.opprettDto = opprettDto;

        JournalfoeringTilordneDto tilordneDto = new JournalfoeringTilordneDto();
        tilordneDto.setJournalpostID("setJournalpostID");
        tilordneDto.setDokumentID("setDokumentID");
        tilordneDto.setOppgaveID("setOppgaveID");
        tilordneDto.setAvsenderNavn("setAvsenderNavn");
        tilordneDto.setAvsenderID("setAvsenderID");
        tilordneDto.setBrukerID("setBrukerID");
        tilordneDto.setHoveddokumentTittel("setDokumenttittel");
        this.tilordneDto = tilordneDto;
    }

    @Test
    public void opprettSakOgJournalfør() throws MelosysException {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.MIN);
        periode.setTom(LocalDate.MAX);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Arrays.asList("DK"));
        opprettDto.setFagsak(fagsakDto);
        journalfoeringService.opprettOgJournalfør(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void opprettSakOgJournalfør_oppgaveID_mangler() throws MelosysException {
        opprettDto.setOppgaveID(null);
        journalfoeringService.opprettOgJournalfør(opprettDto);
    }

    @Test
    public void opprettOgJournalfør_erSed_prosessinstansOpprettet() throws MelosysException {
        journalpost.setMottaksKanal("EESSI");
        journalpost.getHoveddokument().setNavSkjemaID("A009");
        when(eessiService.støtterAutomatiskBehandling(anyString(), anyString())).thenReturn(Boolean.TRUE);

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansSedMottak(anyString(), anyString());
    }

    @Test
    public void tilordneSakOgJournalfør() throws FunksjonellException, TekniskException {
        tilordneDto.setSaksnummer("MEL-0123");
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void tilordneSakOgJournalfør_saksnr_mangler() throws FunksjonellException, TekniskException {
        tilordneDto.setSaksnummer("");
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);
    }

    @Test
    public void valider() throws FunksjonellException {
        journalfoeringService.valider(opprettDto);
    }

    @Test(expected = FunksjonellException.class)
    public void valider_brukerID_mangler() throws FunksjonellException {
        opprettDto.setBrukerID(null);
        journalfoeringService.valider(opprettDto);
    }

    @Test
    public void opprettJournalpostSedSomBrev_forventJournalpostId() throws TekniskException {
        String fnr = "11223344556";
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        byte[] dokument = "et dokument".getBytes();

        when(joarkFasade.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").journalstatus("ENDELIG").build());

        String journalpostId = journalfoeringService.opprettJournalpostSedSomBrev(fagsak, "MED", fnr,
            "mottakernavn", "SWE", "SED A011", "SED A011", dokument, null);

        assertThat(journalpostId).isEqualTo("1234");
    }

    @Test(expected = TekniskException.class)
    public void opprettJournalpostSedSomBrev_forventException() throws TekniskException {
        String fnr = "11223344556";
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        byte[] dokument = "et dokument".getBytes();

        when(joarkFasade.opprettJournalpost(any(OpprettJournalpostRequest.class), anyBoolean()))
            .thenReturn(OpprettJournalpostResponse.builder().journalpostId("1234").journalstatus("MIDLERTIDIG").build());

        journalfoeringService.opprettJournalpostSedSomBrev(fagsak, "MED", fnr, "mottakernavn",
            "SWE", "SED A011", "SED A011", dokument, null);
    }
}