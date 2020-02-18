package no.nav.melosys.integrasjonstest;

import java.util.Collections;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakSystemService;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjonstest.felles.TestSubjectHandler;
import no.nav.melosys.integrasjonstest.felles.opplysninger.MelosysTjenesteGrensesnitt;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger;
import no.nav.melosys.integrasjonstest.felles.verifisering.DokumentSjekker;
import no.nav.melosys.integrasjonstest.felles.verifisering.ProsessinstansTestService;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnmodningUnntakIT {

    @MockBean
    JoarkService joarkService;

    @MockBean
    GsakSystemService gsakSystemService;

    @MockBean
    GsakFasade gsakFasade;

    @MockBean
    OppgaveService oppgaveService;

    @MockBean
    @Qualifier("system")
    EessiService eessiService;

    @Autowired
    private AnmodningUnntakService anmodningUnntakService;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    @Autowired
    private MelosysTjenesteGrensesnitt behandlingsUtfyller;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @Autowired
    private ProsessinstansTestService prosessinstansTestService;

    @BeforeEach
    public void saksflytAnmodningTilVedtakTest() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any(), any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any(Oppgave.class))).thenReturn("");

        prosessinstansRepository.deleteAll();
    }

    @Test
    public void anmodningOmUnntak_anmodningOmUnntakUtenPeriode_skalFeile() throws MelosysException {
        behandlingsUtfyller.setUnderBehandling(Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        anmodningUnntakService.anmodningOmUnntak(Testbehandlinger.UTFYLT_BEHANDLING_ART12, "");
        prosessinstansTestService.ventPå(Testbehandlinger.UTFYLT_BEHANDLING_ART12);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.UTFYLT_BEHANDLING_ART12, FEILET_MASKINELT);

        dokumentSjekker.ingenBrevSendt();
    }
}