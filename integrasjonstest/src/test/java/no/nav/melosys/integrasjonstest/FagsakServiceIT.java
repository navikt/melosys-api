package no.nav.melosys.integrasjonstest;

import java.time.Instant;
import java.util.Collections;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakSystemService;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjonstest.felles.TestSubjectHandler;
import no.nav.melosys.integrasjonstest.felles.opplysninger.MelosysTjenesteGrensesnitt;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger;
import no.nav.melosys.integrasjonstest.felles.opplysninger.TestdataUtfyller;
import no.nav.melosys.integrasjonstest.felles.verifisering.DokumentSjekker;
import no.nav.melosys.integrasjonstest.felles.verifisering.ProsessinstansTestService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner.SOEKNADEN_TRUKKET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_HENLAGT_SAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.AVKLART_ARBEIDSGIVER_ORGNR;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.DELOITTE_ORGNR;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ComponentScan("felles")
@ExtendWith(SpringExtension.class)
public class FagsakServiceIT {

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
    private FagsakService fagsakService;

    @Autowired
    MelosysTjenesteGrensesnitt melosysGrensesnitt;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @Autowired
    private ProsessinstansTestService prosessinstansTestService;

    @BeforeEach
    public void setup() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any(), any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any(Oppgave.class))).thenReturn("");
    }

    @Test
    public void fagsak_henleggFagsak() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);

        fagsakService.henleggFagsak("MELTEST-2", SOEKNADEN_TRUKKET.getKode(), "");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER));
    }

    @Test
    public void fagsak_henleggFagsak_medArbeidsgiver() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.SE, AVKLART_ARBEIDSGIVER_ORGNR);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);

        fagsakService.henleggFagsak("MELTEST-2", SOEKNADEN_TRUKKET.getKode(), "");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER));
        //  Arbeidsgiver skal ikke ha brev: MELOSYS-1731
    }

    @Test
    public void fagsak_henleggFagsak_medRepresentantForArbeidsgiver() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .opprettAktørRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR)
        .utfyllAvklartefaktaForArt12(Landkoder.SE, AVKLART_ARBEIDSGIVER_ORGNR);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);

        fagsakService.henleggFagsak("MELTEST-2", SOEKNADEN_TRUKKET.getKode(), "");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER));
           // Arbeidsgivers representant skal ikke ha brev: MELOSYS-1731
    }

    @Test
    public void fagsak_henleggFagsak_medRepresentantForBruker() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .opprettAktørRepresentant(Representerer.BRUKER, DELOITTE_ORGNR)
        .utfyllAvklartefaktaForArt12(Landkoder.SE, AVKLART_ARBEIDSGIVER_ORGNR);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);

        fagsakService.henleggFagsak("MELTEST-2", SOEKNADEN_TRUKKET.getKode(), "");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER),
            forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
            // Kun bruker og brukers representant skal ha brev: MELOSYS-1731
    }

    @Test
    public void fagsak_henleggFagsak_medRepresentantForBegge() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .opprettAktørRepresentant(Representerer.BEGGE, DELOITTE_ORGNR)
        .utfyllAvklartefaktaForArt12(Landkoder.SE, AVKLART_ARBEIDSGIVER_ORGNR);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);

        fagsakService.henleggFagsak("MELTEST-2", SOEKNADEN_TRUKKET.getKode(), "");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER),
            forventDokument(MELDING_HENLAGT_SAK, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
    }

    @Test
    public void fagsak_henleggVideresend() throws FunksjonellException, TekniskException {
        TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt13(Landkoder.SE, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR, Landkoder.SE);

        Journalpost journalpost = new Journalpost("123123");
        journalpost.setForsendelseMottatt(Instant.now());
        when(joarkService.hentJournalpost(any())).thenReturn(journalpost);
        when(joarkService.hentDokument(any(), any())).thenReturn(new byte[]{});

        fagsakService.henleggOgVideresend("MELTEST-2", "SE:inst1234");
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING, ProsessSteg.VS_SEND_SOKNAD);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(ORIENTERING_VIDERESENDT_SOEKNAD, Aktoersroller.BRUKER));
    }
}