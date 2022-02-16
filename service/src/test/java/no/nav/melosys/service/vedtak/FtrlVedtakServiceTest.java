package no.nav.melosys.service.vedtak;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN_2_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FtrlVedtakServiceTest {

    public static final String SAKSNUMMER = "MEL-123";
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private ProsessinstansService prosessinstansService;

    @Mock
    private OppgaveService oppgaveService;

    @Mock
    private DokgenService dokgenService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    @Captor
    private ArgumentCaptor<BrevbestillingRequest> brevbestillingRequestCaptor;

    private FtrlVedtakService ftrlVedtakService;

    @BeforeEach
    void setup() {
        ftrlVedtakService = new FtrlVedtakService(behandlingsresultatService, behandlingService, prosessinstansService, oppgaveService, dokgenService);

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_Førstegangsvedtak_fatterVedtak() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattFtrlVedtakRequest request = lagFattVedtakRequest();
        ftrlVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting("type", "begrunnelseFritekst", "fastsattAvLand")
            .containsExactly(MEDLEM_I_FOLKETRYGDEN, "Begrunnelse", Landkoder.NO);

        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);

        BrevbestillingRequest brevbestillingRequest = brevbestillingRequestCaptor.getValue();
        assertThat(brevbestillingRequest)
            .extracting("produserbardokument", "bestillersId", "mottaker", "innledningFritekst",
                "begrunnelseFritekst", "ektefelleFritekst", "barnFritekst")
            .containsExactly(INNVILGELSE_FOLKETRYGDLOVEN_2_8, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet");
        assertThat(brevbestillingRequest.getKopiMottakere().size()).isEqualTo(1);
        assertThat(brevbestillingRequest.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
    }

    private FattFtrlVedtakRequest lagFattVedtakRequest() {
        return new FattFtrlVedtakRequest.Builder()
            .medBehandlingsresultat(MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medEktefelleFritekst("Ektefelle omfattet")
            .medBarnFritekst("Barn omfattet")
            .medKopiMottakere(List.of(new KopiMottaker(Aktoersroller.ARBEIDSGIVER, "987654321", null, null)))
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        return fagsak;
    }
}
