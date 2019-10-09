package no.nav.melosys.integrasjonstest;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static org.assertj.core.api.Java6Assertions.assertThat;

@SpringBootTest
@ContextConfiguration()
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class MelosysTest {

    private static final Logger logger = LoggerFactory.getLogger(MelosysTest.class);

    @Autowired
    private BehandlingRepository behandlingRepository;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    @Autowired
    private VedtakService vedtakService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void setup() {
        SpringSubjectHandler.set(new TestSubjectHandler());
//        vedtakService = new VedtakService(behandlingRepository, mock(OppgaveService.class), prosessinstansService);
    }

    @Test
    public void kjøreSaksflyt() throws FunksjonellException, TekniskException, InterruptedException {
        vedtakService.fattVedtak(3L, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Thread.sleep(60000);

        List<ProsessSteg> steg = prosessinstansRepository.findAll().stream()
            .filter(pi -> pi.getBehandling().getId() == 3)
            .map(Prosessinstans::getSteg)
            .collect(Collectors.toList());

        logger.info("Steg: {}", steg);

        assertThat(steg).doesNotContain(FEILET_MASKINELT);

        Thread.sleep(5000);
    }

    private class TestSubjectHandler extends SubjectHandler {
        @Override
        public String getOidcTokenString() {
            return null;
        }

        @Override
        public String getUserID() {
            return "Z990007";
        }
    }
}
