package no.nav.melosys.integrasjonstest;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakSystemService;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjonstest.felles.TestSubjectHandler;
import no.nav.melosys.integrasjonstest.felles.opplysninger.MelosysTjenesteGrensesnitt;
import no.nav.melosys.integrasjonstest.felles.opplysninger.TestdataUtfyller;
import no.nav.melosys.integrasjonstest.felles.verifisering.DokumentSjekker;
import no.nav.melosys.integrasjonstest.felles.verifisering.ProsessinstansTestService;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger.TOM_BEHANDLING_REPRESENTANT;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.*;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class VedtakServiceRepresentantIT {

    @Autowired
    private ProsessinstansRepository prosessinstansRepository;

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

        prosessinstansRepository.deleteAll();
    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantArbeidsgiver() throws MelosysException {
        TestdataUtfyller faktagrunnlag = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Innvilgelse()
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)
        .opprettAktørRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(faktagrunnlag.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
        verify(eessiService).opprettOgSendSed(faktagrunnlag.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_04, null);
    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantBruker() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Innvilgelse()
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)
        .opprettAktørRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
        verify(eessiService).opprettOgSendSed(testdataUtfyller.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_04, null);
    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantBegge() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
            .utfyllVilkaarForArt12Innvilgelse()
            .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)
            .opprettAktørRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
        verify(eessiService).opprettOgSendSed(testdataUtfyller.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_04, null);
    }


    @Test
    public void fattVedtak_innvilgelseArt13_representantArbeidsgiver() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt13BostedNorge(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR)
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)
        .opprettAktørRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR));
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
        verify(eessiService).opprettOgSendSed(testdataUtfyller.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_02, null);
    }

    @Test
    public void fattVedtak_innvilgelseArt13_representantBruker() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt13BostedNorge(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR)
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)
        .opprettAktørRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR));
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
        verify(eessiService).opprettOgSendSed(testdataUtfyller.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_02, null);
    }

    @Test
    public void fattVedtak_innvilgelseArt13_representantBegge() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt13BostedNorge(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR)
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)
        .opprettAktørRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(INSTITUSJONSKODE_ØSTERRIKET), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR));
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
        verify(eessiService).opprettOgSendSed(testdataUtfyller.getBehandlingsid(), List.of(INSTITUSJONSKODE_ØSTERRIKET), BucType.LA_BUC_02, null);
    }

    @Test
    public void fattVedtak_avslagArt12_representantArbeidsgiver() throws MelosysException {
        TestdataUtfyller faktagrunnlag = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Avslag()
        .opprettAvslåttLovvalgsperiode(FO_883_2004_ART12_1)
        .opprettAktørRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(faktagrunnlag.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));

    }

    @Test
    public void fattVedtak_avslagArt12_representantBruker() throws MelosysException {
        TestdataUtfyller testdataUtfyller = new TestdataUtfyller(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Avslag()
        .opprettAvslåttLovvalgsperiode(FO_883_2004_ART12_1)
        .opprettAktørRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_avslagArt12_representantBegge() throws MelosysException {
        TestdataUtfyller faktagrunnlag = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .utfyllVilkaarForArt12Avslag()
        .opprettAvslåttLovvalgsperiode(FO_883_2004_ART12_1)
        .opprettAktørRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(faktagrunnlag.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
    }


    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantArbeidsgiver() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
            .opprettAktørRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
    }

    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantBruker() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
            .opprettAktørRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantBegge() throws MelosysException {
        TestdataUtfyller testdataUtfyller = TestdataUtfyller.til(TOM_BEHANDLING_REPRESENTANT, melosysGrensesnitt)
            .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
            .opprettAktørRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(testdataUtfyller.getBehandlingsid(), Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "", List.of(), Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        prosessinstansTestService.ventPå(testdataUtfyller.getBehandlingsid());
        prosessinstansTestService.sjekkProsessteg(testdataUtfyller.getBehandlingsid(), FERDIG);

        dokumentSjekker.sjekkBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
    }
}