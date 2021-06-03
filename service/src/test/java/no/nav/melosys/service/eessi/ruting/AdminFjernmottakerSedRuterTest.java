package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminFjernmottakerSedRuterTest {
    @Mock
    private FagsakService fagsakService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private AdminFjernmottakerSedRuter adminFjernmottakerSedRuter;

    private final long behandlingID = 111;
    private final long arkivsakID = 123321;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
    private final String rinaSaksnummer = "1233333";
    private final String sedID = "2414";

    @BeforeEach
    void setup() {
        adminFjernmottakerSedRuter = new AdminFjernmottakerSedRuter(fagsakService, prosessinstansService, oppgaveService,
            behandlingsresultatService, medlPeriodeService);

        melosysEessiMelding.setAktoerId("12312412");
        melosysEessiMelding.setRinaSaksnummer("143141");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_arkivsaksIdErNull_opprettJournalFøringsOppgave(){
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());

    }

    @Test
    void rutSedTilBehandling_finnesIngenTilhørendeFagsak_opprettesJfrOppgave() {
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

}
