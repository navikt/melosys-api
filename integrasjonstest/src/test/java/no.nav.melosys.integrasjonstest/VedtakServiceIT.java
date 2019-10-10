package no.nav.melosys.integrasjonstest;

import java.util.Collections;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
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

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag.SOEKT_FOR_SENT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjonstest.felles.utils.SaksflytTestUtils.*;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ForventetDokumentBestilling.forventDokument;
import static no.nav.melosys.integrasjonstest.felles.verifisering.ResultatPoller.Resultatpoller;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class VedtakServiceIT {

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
    public void fattVedtak_innvilgelse() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        vedtakService.fattVedtak(Testbehandlinger.UTFYLT_BEHANDLING_ART12, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        dokumentSjekker.erBrevBestilt(
            forventDokument(ATTEST_A1, Aktoersroller.MYNDIGHET),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(INNVILGELSE_YRKESAKTIV, Aktoersroller.MYNDIGHET),
            forventDokument(INNVILGELSE_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER));

        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12, FERDIG);
    }

    @Test
    public void fattVedtak_avslag() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12);

        behandlingsdata.opprettVilkaar(Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12, lagVilkaarDto(Vilkaar.FO_883_2004_ART16_1, false, SOEKT_FOR_SENT));
        behandlingsdata.opprettLovvalgsperiode(Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12, lagLovvalgsperiodeDto(FO_883_2004_ART12_1, Landkoder.DK, InnvilgelsesResultat.AVSLAATT));

        vedtakService.fattVedtak(Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, "");
        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET),  // Helfo
            forventDokument(AVSLAG_YRKESAKTIV, Aktoersroller.MYNDIGHET),  // Skatt
            forventDokument(AVSLAG_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER));

        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.FAKTAUTFYLT_BEHANDLING_ART12, FERDIG);
    }

    @Test
    public void fattVedtak_avslag_manglende_opplysninger() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.TOM_BEHANDLING);
        vedtakService.fattVedtak(Testbehandlinger.TOM_BEHANDLING, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, "");
        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.TOM_BEHANDLING);

        dokumentSjekker.erBrevBestilt(
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.BRUKER),
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET),  // Helfo
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.MYNDIGHET),  // Skatt
            forventDokument(AVSLAG_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER));

        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.TOM_BEHANDLING, FERDIG);
    }

    @Test
    public void fattVedtak_anmodningOmUnntak_skalFeile() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.UTFYLT_BEHANDLING_ART12);
        vedtakService.fattVedtak(Testbehandlinger.UTFYLT_BEHANDLING_ART12, Behandlingsresultattyper.ANMODNING_OM_UNNTAK, "");
        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        verify(dokumentSjekker.getDoksysService(), never()).produserIkkeredigerbartDokument(any());
        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12, FEILET_MASKINELT);
    }

    @Test
    public void fattVedtak_henleggelse_skalFeile() throws FunksjonellException, TekniskException, InterruptedException {
        behandlingsdata.setUnderBehandling(Testbehandlinger.UTFYLT_BEHANDLING_ART12);
        vedtakService.fattVedtak(Testbehandlinger.UTFYLT_BEHANDLING_ART12, Behandlingsresultattyper.HENLEGGELSE, "");

        Resultatpoller().følg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12);

        verify(dokumentSjekker.getDoksysService(), never()).produserIkkeredigerbartDokument(any());
        sjekkProsessteg(prosessinstansRepository, Testbehandlinger.UTFYLT_BEHANDLING_ART12, FEILET_MASKINELT);
    }
}