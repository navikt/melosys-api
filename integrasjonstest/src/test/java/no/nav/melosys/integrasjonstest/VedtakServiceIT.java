package no.nav.melosys.integrasjonstest;

import java.util.Collections;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
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
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class VedtakServiceIT {

    private static final String HELFO_ORGNR = "986965610";
    private static final String SKATTEETATEN_ORGNR = "974761076";

    private static final String INSTITUSJONSKODE_ØSTERRIKET = "AT:9600";
    private static final String AVKLART_ARBEIDSGIVER_ORGNR = "982683955";

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
    private VedtakService vedtakService;

    @Autowired
    private MelosysTjenesteGrensesnitt melosysGrensesnitt;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @Autowired
    private ProsessinstansTestService prosessinstansTestService;

    @BeforeEach
    public void setup() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any(), any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any(Oppgave.class))).thenReturn("");

        prosessinstansTestService.nullstill();
    }

    @Test
    public void fattVedtak_innvilgelse_art12() throws MelosysException {
        TestdataUtfyller utfyller = TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Innvilgelse()
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART12_1);

        vedtakService.fattVedtak(utfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", "", Vedtakstyper.FØRSTEGANGSVEDTAK, null);

        prosessinstansTestService.ventPå(utfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(utfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_innvilgelseBostedNorgeArbeidPåNorskflaggetSkip_Art113A() throws MelosysException {
        TestdataUtfyller utfyller = TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING_MARITIMT_ARBEID_OG_OPPGITT_ADRESSE, melosysGrensesnitt)
            .utfyllAvklartefaktaArbeidPåSkip(Landkoder.NO, Landkoder.NO, "Seven Kestrel", AVKLART_ARBEIDSGIVER_ORGNR)
            .utfyllVilkaarForArt113A()
            .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART11_3A);

        vedtakService.fattVedtak(utfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", "");

        prosessinstansTestService.ventPå(utfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(utfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_innvilgelseBostedØsterriketArbeidPåNorskflaggetSkip_Art113A() throws MelosysException {
        TestdataUtfyller utfyller = TestdataUtfyller.til(Testbehandlinger.TOM_BEHANDLING_MARITIMT_ARBEID_OG_OPPGITT_ADRESSE, melosysGrensesnitt)
            .utfyllAvklartefaktaArbeidPåSkip(Landkoder.AT, Landkoder.NO, "Seven Kestrel", AVKLART_ARBEIDSGIVER_ORGNR)
            .utfyllVilkaarForArt113A()
            .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART11_3A);

        vedtakService.fattVedtak(utfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", "");

        prosessinstansTestService.ventPå(utfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(utfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_innvilgelseNorgeBostedsland_art13() throws MelosysException {
        TestdataUtfyller testDataUtfyller = new TestdataUtfyller(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt13BostedNorge(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR)
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A);

        vedtakService.fattVedtak(testDataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", "", Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testDataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testDataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR)
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
        );
    }

    @Test
    public void fattVedtak_avslag_art12() throws MelosysException {
        TestdataUtfyller testDataUtfyller = new TestdataUtfyller(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Avslag()
        .opprettAvslåttLovvalgsperiode(FO_883_2004_ART12_1);

        vedtakService.fattVedtak(testDataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", "", Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testDataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testDataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_avslagManglendeOpplysninger_ingenAvklartArbeidsgiver() throws MelosysException {
        new TestdataUtfyller(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt);

        vedtakService.fattVedtak(Testbehandlinger.TOM_BEHANDLING, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR));
    }

    @Test
    public void fattVedtak_avslagManglendeOpplysningerMedArbeidsgiver_sendesOgsåTilArbGiver() throws MelosysException {
        new TestdataUtfyller(Testbehandlinger.TOM_BEHANDLING, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt12(Landkoder.SE, AVKLART_ARBEIDSGIVER_ORGNR);

        vedtakService.fattVedtak(Testbehandlinger.TOM_BEHANDLING, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,"", "", Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(Testbehandlinger.TOM_BEHANDLING);
        prosessinstansTestService.sjekkProsessteg(Testbehandlinger.TOM_BEHANDLING, FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }
}