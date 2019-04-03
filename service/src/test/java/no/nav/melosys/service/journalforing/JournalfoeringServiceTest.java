package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Arrays;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JournalfoeringServiceTest {

    @Mock
    private JoarkFasade joarkFasade;

    @Mock
    private ProsessinstansService prosessinstansService;

   @Mock
    private OppgaveService oppgaveService;

    private JournalfoeringService journalfoeringService;

    private JournalfoeringOpprettDto opprettDto;

    private JournalfoeringTilordneDto tilordneDto;

    @Before
    public void setup() {
        this.journalfoeringService = new JournalfoeringService(joarkFasade, oppgaveService, prosessinstansService);
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
    public void opprettSakOgJournalfør() throws FunksjonellException, TekniskException {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.MIN);
        periode.setTom(LocalDate.MAX);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Arrays.asList("DK"));
        opprettDto.setFagsak(fagsakDto);
        journalfoeringService.opprettSakOgJournalfør(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void opprettSakOgJournalfør_oppgaveID_mangler() throws FunksjonellException, TekniskException {
        opprettDto.setOppgaveID(null);
        journalfoeringService.opprettSakOgJournalfør(opprettDto);
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
}