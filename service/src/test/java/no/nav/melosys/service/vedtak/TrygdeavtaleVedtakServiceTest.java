package no.nav.melosys.service.vedtak;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_NO_UK_1;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleVedtakServiceTest {

    public static final long BEHANDLING_ID = 123L;
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
    @Mock
    private VedtakKontrollService vedtakKontrollService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;
    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;
    @Captor
    private ArgumentCaptor<BrevbestillingRequest> brevbestillingRequestCaptor;

    private TrygdeavtaleVedtakService trygdeavtaleVedtakService;

    @BeforeEach
    void setup() {
        trygdeavtaleVedtakService = new TrygdeavtaleVedtakService(behandlingsresultatService, behandlingService, prosessinstansService, oppgaveService, dokgenService, vedtakKontrollService);

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_førstegangsvedtak_fatterVedtak() throws ValideringException {
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        FattTrygdeavtaleVedtakRequest request = lagFattVedtakRequest();
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).oppdaterStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakTrygdeavtale(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(vedtakKontrollService).validerInnvilgelse(any(Behandling.class), any(Behandlingsresultat.class), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), eq(Sakstyper.TRYGDEAVTALE));

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting("type", "begrunnelseFritekst", "fastsattAvLand")
            .containsExactly(FASTSATT_LOVVALGSLAND, "Begrunnelse", Landkoder.NO);

        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);

        BrevbestillingRequest brevbestillingRequest = brevbestillingRequestCaptor.getValue();
        assertThat(brevbestillingRequest)
            .extracting("produserbardokument", "bestillersId", "mottaker", "innledningFritekst",
                "begrunnelseFritekst", "ektefelleFritekst", "barnFritekst")
            .containsExactly(ATTEST_NO_UK_1, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet");
        assertThat(brevbestillingRequest.getKopiMottakere().size()).isEqualTo(1);
        assertThat(brevbestillingRequest.getKopiMottakere().get(0).getRolle()).isEqualTo(ARBEIDSGIVER);
    }

    private FattTrygdeavtaleVedtakRequest lagFattVedtakRequest() {
        return new FattTrygdeavtaleVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medFritekstInnledning("Innledning")
            .medFritekstBegrunnelse("Begrunnelse")
            .medFritekstEktefelle("Ektefelle omfattet")
            .medFritekstBarn("Barn omfattet")
            .medKopiMottakere(List.of(new KopiMottaker(ARBEIDSGIVER, "987654321", null)))
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
    }

    private Behandling lagBehandling() {
        var behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    private Fagsak lagFagsak() {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        return fagsak;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.GB);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        return behandlingsresultat;
    }
}
