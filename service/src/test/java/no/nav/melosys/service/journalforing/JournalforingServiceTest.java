package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Arrays;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.journalforing.dto.FagsakDto;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JournalforingServiceTest {

    @Mock
    private Binge binge;

    @Mock
    private JoarkFasade joarkFasade;

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    private JournalforingService journalforingService;

    private JournalforingDto dto;

    @Before
    public void setup() {
        this.journalforingService = new JournalforingService(binge, joarkFasade, prosessinstansRepo);
        JournalforingDto dto = new JournalforingDto();
        dto.setJournalpostID("setJournalpostID");
        dto.setDokumentID("setDokumentID");
        dto.setOppgaveID("setOppgaveID");
        dto.setAvsenderNavn("setAvsenderNavn");
        dto.setAvsenderID("setAvsenderID");
        dto.setBrukerID("setBrukerID");
        dto.setDokumenttittel("setDokumenttittel");
        this.dto = dto;

    }

    @Test
    public void opprettSakOgJournalfør() throws FunksjonellException {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.MIN);
        periode.setTom(LocalDate.MAX);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Arrays.asList("DK", "NO"));
        dto.setFagsak(fagsakDto);
        journalforingService.opprettSakOgJournalfør(dto);
    }

    @Test(expected = FunksjonellException.class)
    public void opprettSakOgJournalfør_oppgaveID_mangler() throws FunksjonellException {
        dto.setOppgaveID(null);
        journalforingService.opprettSakOgJournalfør(dto);
    }

    @Test
    public void tilordneSakOgJournalfør() throws FunksjonellException {
        dto.setSaksnummer("MEL-0123");
        journalforingService.tilordneSakOgJournalfør(dto);
    }

    @Test(expected = FunksjonellException.class)
    public void tilordneSakOgJournalfør_saksnr_mangler() throws FunksjonellException {
        dto.setSaksnummer("");
        journalforingService.tilordneSakOgJournalfør(dto);
    }

    @Test
    public void valider() throws FunksjonellException {
        journalforingService.valider(dto);
    }

    @Test(expected = FunksjonellException.class)
    public void valider_brukerID_mangler() throws FunksjonellException {
        dto.setBrukerID(null);
        journalforingService.valider(dto);
    }
}