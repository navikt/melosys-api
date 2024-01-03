package no.nav.melosys.service.vedtak;

import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
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

import static no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    @Captor
    private ArgumentCaptor<BrevbestillingDto> brevbestillingRequestCaptor;

    private FtrlVedtakService ftrlVedtakService;

    @BeforeEach
    void setup() {
        ftrlVedtakService = new FtrlVedtakService(behandlingsresultatService, behandlingService, prosessinstansService, oppgaveService, dokgenService, vilkaarsresultatService);

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
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request.tilVedtakRequest()));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());

        assertThat(behandlingsresultatCaptor.getValue())
            .extracting(
                Behandlingsresultat::getType,
                Behandlingsresultat::getBegrunnelseFritekst,
                Behandlingsresultat::getFastsattAvLand
            )
            .containsExactly(MEDLEM_I_FOLKETRYGDEN, "Begrunnelse", Land_iso2.NO);

        assertThat(behandlingCaptor.getValue().getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);

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
            .containsExactly(
                INNVILGELSE_FOLKETRYGDLOVEN,
                "Z990007",
                BRUKER,
                "Innledning",
                "Begrunnelse",
                "Ektefelle omfattet",
                "Barn omfattet");
        assertThat(brevbestillingDto.getKopiMottakere()).hasSize(2);
        assertThat(brevbestillingDto.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
        assertThat(brevbestillingDto.getKopiMottakere().get(1).rolle()).isEqualTo(UTENLANDSK_TRYGDEMYNDIGHET);
    }

    @Test
    void fattVedtak_avslag_manglende_opplysninger_fatterVedtak() {
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(AVSLAG_MANGLENDE_OPPL)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("fritekst for beskrivelse avslag")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();


        ftrlVedtakService.fattVedtak(lagBehandling(), request);


        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request.tilVedtakRequest()));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());

        assertThat(behandlingsresultatCaptor.getValue().getType()).isEqualTo(AVSLAG_MANGLENDE_OPPL);
        assertThat(behandlingCaptor.getValue().getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);
        assertThat(brevbestillingRequestCaptor.getValue())
            .extracting(
                BrevbestillingDto::getProduserbardokument,
                BrevbestillingDto::getBestillersId,
                BrevbestillingDto::getMottaker,
                BrevbestillingDto::getFritekst,
                BrevbestillingDto::getKopiMottakere
            )
            .containsExactly(
                AVSLAG_MANGLENDE_OPPLYSNINGER,
                "Z990007",
                BRUKER,
                "fritekst for beskrivelse avslag",
                List.of());
    }

    @Test
    void fattVedtak_delvis_opphørt_fatterVedtak() {
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(DELVIS_OPPHØRT)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("fritekst for begrunnelse")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();


        ftrlVedtakService.fattVedtak(lagBehandling(), request);


        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request.tilVedtakRequest()));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(vilkaarsresultatService, never()).tømVilkårForBehandlingsresultat(any());

        assertThat(behandlingsresultatCaptor.getValue().getType()).isEqualTo(DELVIS_OPPHØRT);
        assertThat(behandlingCaptor.getValue().getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);
        assertThat(brevbestillingRequestCaptor.getValue())
            .extracting(
                BrevbestillingDto::getProduserbardokument,
                BrevbestillingDto::getBestillersId,
                BrevbestillingDto::getMottaker,
                BrevbestillingDto::getBegrunnelseFritekst,
                BrevbestillingDto::getKopiMottakere
            )
            .containsExactly(
                VEDTAK_OPPHOERT_MEDLEMSKAP,
                "Z990007",
                BRUKER,
                "fritekst for begrunnelse",
                List.of());
    }

    @Test
    void fattVedtak_opphørt_fatterVedtak() {
        var avklartFakta = new Avklartefakta();
        avklartFakta.setType(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING);
        avklartFakta.setReferanse(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.getKode());
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(Sets.newHashSet(avklartFakta, new Avklartefakta()));
        behandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        behandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(Sets.newHashSet(new Medlemskapsperiode()));
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);
        behandlingsresultat.setNyVurderingBakgrunn("blah");
        behandlingsresultat.setInnledningFritekst("blah");
        behandlingsresultat.setBegrunnelseFritekst("blah");
        behandlingsresultat.setTrygdeavgiftFritekst("blah");

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(OPPHØRT)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medBegrunnelseFritekst("fritekst for begrunnelse")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();


        ftrlVedtakService.fattVedtak(lagBehandling(), request);


        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakFTRL(any(Behandling.class), eq(request.tilVedtakRequest()));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(vilkaarsresultatService).tømVilkårForBehandlingsresultat(any());

        var capturedBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(capturedBehandlingsresultat.getType()).isEqualTo(OPPHØRT);
        assertThat(capturedBehandlingsresultat.getMedlemAvFolketrygden().getBestemmelse()).isEqualTo(FTRL_KAP2_2_15_ANDRE_LEDD);
        assertThat(capturedBehandlingsresultat.getMedlemAvFolketrygden().getMedlemskapsperioder()).isEmpty();
        assertThat(capturedBehandlingsresultat.getAvklartefakta()).hasSize(1);
        assertThat(capturedBehandlingsresultat.getUtfallRegistreringUnntak()).isNull();
        assertThat(capturedBehandlingsresultat.getNyVurderingBakgrunn()).isNull();
        assertThat(capturedBehandlingsresultat.getInnledningFritekst()).isNull();
        assertThat(capturedBehandlingsresultat.getTrygdeavgiftFritekst()).isNull();
        assertThat(capturedBehandlingsresultat.getBegrunnelseFritekst()).isEqualTo(request.getBegrunnelseFritekst());
        assertThat(capturedBehandlingsresultat.getFastsattAvLand()).isEqualTo(Land_iso2.NO);
        assertThat(capturedBehandlingsresultat.getVedtakMetadata().getVedtakstype()).isEqualTo(request.getVedtakstype());
        assertThat(behandlingCaptor.getValue().getFagsak().getStatus()).isEqualTo(Saksstatuser.OPPHØRT);
        assertThat(brevbestillingRequestCaptor.getValue())
            .extracting(
                BrevbestillingDto::getProduserbardokument,
                BrevbestillingDto::getBestillersId,
                BrevbestillingDto::getMottaker,
                BrevbestillingDto::getBegrunnelseFritekst,
                BrevbestillingDto::getKopiMottakere
            )
            .containsExactly(
                VEDTAK_OPPHOERT_MEDLEMSKAP,
                "Z990007",
                BRUKER,
                "fritekst for begrunnelse",
                List.of());
    }

    @Test
    void fattVedtak_opphørt_manglerAvklartFakta_kasterFeil() {
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(new Behandlingsresultat());
        var fattVedtakRequest = new FattVedtakRequest.Builder().medBehandlingsresultatType(OPPHØRT).build();
        var behandling = lagBehandling();


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> ftrlVedtakService.fattVedtak(behandling, fattVedtakRequest))
            .withMessageContaining("Forventer at fullstendigManglendeInnbetaling er satt ved fatting av vedtak for behandlingstype OPPHØRT");
    }


    private FattVedtakRequest lagFattVedtakRequest() {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medEktefelleFritekst("Ektefelle omfattet")
            .medBarnFritekst("Barn omfattet")
            .medKopiMottakere(List.of(
                new KopiMottakerDto(ARBEIDSGIVER, "987654321", null, null),
                new KopiMottakerDto(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "GB:UK010")
            ))
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
