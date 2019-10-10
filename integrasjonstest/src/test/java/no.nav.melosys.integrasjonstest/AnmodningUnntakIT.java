package no.nav.melosys.integrasjonstest;

import java.util.Collections;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakSystemService;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjonstest.felles.TestSubjectHandler;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Behandlingsdata;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger;
import no.nav.melosys.integrasjonstest.felles.verifisering.DokumentSjekker;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.integrasjonstest.felles.utils.SaksflytTestUtils.sjekkProsessteg;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ResultatPoller.Resultatpoller;
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
    EessiService eessiService;

    @Autowired
    private AnmodningUnntakService anmodningUnntakService;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    @Autowired
    private Behandlingsdata behandlingsdata;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @BeforeEach
    public void saksflytAnmodningTilVedtakTest() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any())).thenReturn("");

        prosessinstansRepository.deleteAll();
    }

    @Test
    public void anmodningOmUnntak_anmodningOmUnntakUtenPeriode_skalFeile() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        anmodningUnntakService.anmodningOmUnntak(Testbehandlinger.UTFYLT_BEHANDLING_ART12);
        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        dokumentSjekker.ingenBrevSendt();
        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12, FEILET_MASKINELT);
    }
}