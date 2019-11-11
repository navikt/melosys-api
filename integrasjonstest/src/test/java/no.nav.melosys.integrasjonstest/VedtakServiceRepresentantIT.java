package no.nav.melosys.integrasjonstest;

import java.util.Collections;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakSystemService;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.integrasjonstest.felles.TestSubjectHandler;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Behandlingsdata;
import no.nav.melosys.integrasjonstest.felles.opplysninger.Faktagrunnlag;
import no.nav.melosys.integrasjonstest.felles.verifisering.DokumentSjekker;
import no.nav.melosys.repository.ProsessinstansRepository;
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
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger.TOM_BEHANDLING_REPRESENTANT;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.*;
import static no.nav.melosys.integrasjonstest.felles.utils.SaksflytTestUtils.sjekkProsessteg;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ResultatPoller.Resultatpoller;
import static org.mockito.ArgumentMatchers.any;
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
    EessiService eessiService;

    @Autowired
    private VedtakService vedtakService;

    @Autowired
    private Behandlingsdata behandlingsdata;

    @Autowired
    private DokumentSjekker dokumentSjekker;

    @BeforeEach
    public void setup() throws MelosysException {
        SpringSubjectHandler.set(new TestSubjectHandler());
        when(eessiService.hentEessiMottakerinstitusjoner(any())).thenReturn(Collections.emptyList());
        when(gsakFasade.opprettOppgave(any())).thenReturn("");

        prosessinstansRepository.deleteAll();
    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantArbeidsgiver() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Innvilgelse();
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));

    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantBruker() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Innvilgelse();
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR),
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET));

    }

    @Test
    public void fattVedtak_innvilgelseArt12_representantBegge() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Innvilgelse();
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
        forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET));

    }


    @Test
    public void fattVedtak_innvilgelseArt13_representantArbeidsgiver() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt13(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET));
    }

    @Test
    public void fattVedtak_innvilgelseArt13_representantBruker() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt13(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET));
    }

    @Test
    public void fattVedtak_innvilgelseArt13_representantBegge() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt13(Landkoder.AT, Landkoder.NO, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.utfyllInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(INNVILGELSE_YRKESAKTIV_FLERE_LAND, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            // HELFO skal ikke ha kopi ved Art13 (MELOSYS-3039)
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET, INSTITUSJONSKODE_ØSTERRIKET));
    }

    @Test
    public void fattVedtak_avslagArt12_representantArbeidsgiver() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Avslag();
        faktagrunnlag.utfyllAvslåttLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));

    }

    @Test
    public void fattVedtak_avslagArt12_representantBruker() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Avslag();
        faktagrunnlag.utfyllAvslåttLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));

    }

    @Test
    public void fattVedtak_avslagArt12_representantBegge() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.avklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR);
        faktagrunnlag.vilkaarForArt12Avslag();
        faktagrunnlag.utfyllAvslåttLovvalgsperiode(FO_883_2004_ART12_1);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(faktagrunnlag.getBehandlingsid(), Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, faktagrunnlag.getBehandlingsid());
        sjekkProsessteg(prosessinstansRepository, faktagrunnlag.getBehandlingsid(), FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));

    }


    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantArbeidsgiver() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.ARBEIDSGIVER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(TOM_BEHANDLING_REPRESENTANT, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "");
        Resultatpoller().følg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT);
        sjekkProsessteg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT, FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));

    }

    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantBruker() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BRUKER, DELOITTE_ORGNR);

        vedtakService.fattVedtak(TOM_BEHANDLING_REPRESENTANT, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "");
        Resultatpoller().følg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT);
        sjekkProsessteg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT, FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER, AVKLART_ARBEIDSGIVER_ORGNR));
    }

    @Test
    public void fattVedtak_avslagManglendeOpplysninger_representantBegge() throws FunksjonellException, TekniskException, InterruptedException {
        Faktagrunnlag faktagrunnlag = new Faktagrunnlag(TOM_BEHANDLING_REPRESENTANT, behandlingsdata);
        faktagrunnlag.opprettAktørForBrukerOgRepresentant(Representerer.BEGGE, DELOITTE_ORGNR);

        vedtakService.fattVedtak(TOM_BEHANDLING_REPRESENTANT, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "");
        Resultatpoller().følg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT);
        sjekkProsessteg(prosessinstansRepository, TOM_BEHANDLING_REPRESENTANT, FERDIG);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, HELFO_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET, SKATTEETATEN_ORGNR),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.REPRESENTANT, DELOITTE_ORGNR));
    }
}