package no.nav.melosys.service.vedtak;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.oppgave.OppgaveService;
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

import static no.nav.melosys.domain.kodeverk.Mottakerroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN;
import static org.assertj.core.api.Assertions.assertThat;
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
    private ArgumentCaptor<BrevbestillingDto> brevbestillingRequestCaptor;

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

        FattVedtakRequest request = lagFattVedtakRequest();
        ftrlVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting(
                Behandlingsresultat::getType,
                Behandlingsresultat::getBegrunnelseFritekst,
                Behandlingsresultat::getFastsattAvLand
            )
            .containsExactly(MEDLEM_I_FOLKETRYGDEN, "Begrunnelse", Land_iso2.NO);

        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);

        BrevbestillingDto brevbestillingDto = brevbestillingRequestCaptor.getValue();
        assertThat(brevbestillingDto)
            .extracting(
                BrevbestillingDto::getProduserbardokument,
                BrevbestillingDto::getBestillersId,
                BrevbestillingDto::getMottaker,
                BrevbestillingDto::getInnledningFritekst,
                BrevbestillingDto::getBegrunnelseFritekst,
                BrevbestillingDto::getEktefelleFritekst,
                BrevbestillingDto::getBarnFritekst
            )
            .containsExactly(INNVILGELSE_FOLKETRYGDLOVEN, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet");
        assertThat(brevbestillingDto.getKopiMottakere()).hasSize(1);
        assertThat(brevbestillingDto.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
    }

    @Test
    void fattVedtak_avslag_manglende_opplysninger_fatterVedtak() {
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultat(AVSLAG_MANGLENDE_OPPL)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("fritekst for beskrivelse avslag")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();

        ftrlVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat.getType()).isEqualTo(AVSLAG_MANGLENDE_OPPL);

        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);

        BrevbestillingDto brevbestillingDto = brevbestillingRequestCaptor.getValue();
        assertThat(brevbestillingDto)
            .extracting(
                BrevbestillingDto::getProduserbardokument,
                BrevbestillingDto::getBestillersId,
                BrevbestillingDto::getMottaker,
                BrevbestillingDto::getFritekst
            )
            .containsExactly(AVSLAG_MANGLENDE_OPPLYSNINGER, "Z990007", BRUKER, "fritekst for beskrivelse avslag");
        assertThat(brevbestillingDto.getKopiMottakere()).isEmpty();
    }

    private FattVedtakRequest lagFattVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medEktefelleFritekst("Ektefelle omfattet")
            .medBarnFritekst("Barn omfattet")
            .medKopiMottakere(List.of(new KopiMottakerDto(Mottakerroller.ARBEIDSGIVER, "987654321", null, null)))
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
