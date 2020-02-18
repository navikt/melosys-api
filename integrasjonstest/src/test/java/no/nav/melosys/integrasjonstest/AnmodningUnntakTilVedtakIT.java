package no.nav.melosys.integrasjonstest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
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
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.ANMODNING_UNNTAK_SENDT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.INSTITUSJONSKODE_ØSTERRIKET;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnmodningUnntakTilVedtakIT {

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
    private VedtakService vedtakService;

    @Autowired
    private AnmodningUnntakService anmodningUnntakService;

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

    @Autowired
    private BehandlingRepository behandlingRepository;

    @Autowired
    private MelosysTjenesteGrensesnitt behandlingsUtfyller;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @Autowired
    private ProsessinstansTestService prosessinstansTestService;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void saksflytAnmodningTilVedtakTest() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any(), any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any(Oppgave.class))).thenReturn("");

        prosessinstansRepository.deleteAll();
    }


    // Denne testen bruker testdata direkte.
    // Kjører migrering på nytt for å sikre at dataene er rene.
    @Test
    @Order(0)
    void nullstillDb() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    @Order(1)
    void anmodningOmUnntak_anmodningOmUnntakMedAnmodningsperiode() throws MelosysException {
        Oppgave oppgave = new Oppgave.Builder().setFristFerdigstillelse(LocalDate.now()).build();
        when(gsakSystemService.hentOppgaveMedSaksnummer(any())).thenReturn(oppgave);
        anmodningUnntakService.anmodningOmUnntak(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12, INSTITUSJONSKODE_ØSTERRIKET);

        prosessinstansTestService.ventPå(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12, FERDIG);

        Behandling behandling = behandlingRepository.findById(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12).get();
        assertThat(behandling.getStatus()).isEqualTo(ANMODNING_UNNTAK_SENDT);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(ORIENTERING_ANMODNING_UNNTAK, Aktoersroller.BRUKER)
        );
        verify(eessiService).opprettOgSendSed(behandling.getId(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_01, null);
    }

    @Test
    @Order(2)
    void fattVedtak_anmodningOmUnntak() throws MelosysException {
        behandlingsUtfyller.lagreAnmodningsperiodeSvar(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12, Anmodningsperiodesvartyper.INNVILGELSE);

        vedtakService.fattVedtak(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        prosessinstansTestService.ventPå(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.UTFYLT_BEHANDLING_ART16_UTEN_ART12, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET)
        );
    }
}