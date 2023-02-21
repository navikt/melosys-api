package no.nav.melosys.service.vedtak;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
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

import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.Vedtakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB;
import static org.assertj.core.api.Assertions.assertThat;
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
    private FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;
    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;
    @Captor
    private ArgumentCaptor<BrevbestillingDto> brevbestillingRequestCaptor;

    private TrygdeavtaleVedtakService trygdeavtaleVedtakService;

    @BeforeEach
    void setup() {
        trygdeavtaleVedtakService = new TrygdeavtaleVedtakService(behandlingsresultatService, behandlingService, prosessinstansService, oppgaveService, dokgenService, ferdigbehandlingKontrollFacade);

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_førstegangsvedtak_fatterVedtak() throws ValideringException {
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        FattVedtakRequest request = lagFattVedtakRequest(FØRSTEGANGSVEDTAK, null);
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakTrygdeavtale(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(ferdigbehandlingKontrollFacade).kontrollerVedtakMedRegisteropplysninger(any(Behandling.class), any(Behandlingsresultat.class), eq(Sakstyper.TRYGDEAVTALE), any(Behandlingsresultattyper.class));

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting(
                Behandlingsresultat::getType,
                Behandlingsresultat::getBegrunnelseFritekst,
                Behandlingsresultat::getFastsattAvLand
            )
            .containsExactly(FASTSATT_LOVVALGSLAND, "Begrunnelse", Landkoder.NO);

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
                BrevbestillingDto::getBarnFritekst,
                BrevbestillingDto::getNyVurderingBakgrunn
            )
            .containsExactly(TRYGDEAVTALE_GB, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet", null);
        assertThat(brevbestillingDto.getKopiMottakere()).hasSize(2);
        assertThat(brevbestillingDto.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
        assertThat(brevbestillingDto.getKopiMottakere().get(1).rolle()).isEqualTo(UTENLANDSK_TRYGDEMYNDIGHET);
    }

    @Test
    void fattVedtak_korrigert_vedtak_fatterVedtak() throws ValideringException {
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        FattVedtakRequest request = lagFattVedtakRequest(KORRIGERT_VEDTAK, Nyvurderingbakgrunner.FEIL_I_BEHANDLING.getKode());
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakTrygdeavtale(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(ferdigbehandlingKontrollFacade).kontrollerVedtakMedRegisteropplysninger(any(Behandling.class), any(Behandlingsresultat.class), eq(Sakstyper.TRYGDEAVTALE), any(Behandlingsresultattyper.class));

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting(
                Behandlingsresultat::getType,
                Behandlingsresultat::getBegrunnelseFritekst,
                Behandlingsresultat::getFastsattAvLand
            )
            .containsExactly(FASTSATT_LOVVALGSLAND, "Begrunnelse", Landkoder.NO);

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
                BrevbestillingDto::getBarnFritekst,
                BrevbestillingDto::getNyVurderingBakgrunn
            )
            .containsExactly(TRYGDEAVTALE_GB, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet", Nyvurderingbakgrunner.FEIL_I_BEHANDLING.getKode());
        assertThat(brevbestillingDto.getKopiMottakere()).hasSize(2);
        assertThat(brevbestillingDto.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
        assertThat(brevbestillingDto.getKopiMottakere().get(1).rolle()).isEqualTo(UTENLANDSK_TRYGDEMYNDIGHET);
    }

    @Test
    void fattVedtak_endringsvedtak_fatterVedtak() throws ValideringException {
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        FattVedtakRequest request = lagFattVedtakRequest(ENDRINGSVEDTAK, Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode());
        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakTrygdeavtale(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(dokgenService).produserOgDistribuerBrev(anyLong(), brevbestillingRequestCaptor.capture());
        verify(ferdigbehandlingKontrollFacade).kontrollerVedtakMedRegisteropplysninger(any(Behandling.class), any(Behandlingsresultat.class), eq(Sakstyper.TRYGDEAVTALE), any(Behandlingsresultattyper.class));

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting(
                Behandlingsresultat::getType,
                Behandlingsresultat::getBegrunnelseFritekst,
                Behandlingsresultat::getFastsattAvLand
            )
            .containsExactly(FASTSATT_LOVVALGSLAND, "Begrunnelse", Landkoder.NO);

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
                BrevbestillingDto::getBarnFritekst,
                BrevbestillingDto::getNyVurderingBakgrunn
            )
            .containsExactly(TRYGDEAVTALE_GB, "Z990007", BRUKER, "Innledning",
                "Begrunnelse", "Ektefelle omfattet", "Barn omfattet", Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode());
        assertThat(brevbestillingDto.getKopiMottakere()).hasSize(2);
        assertThat(brevbestillingDto.getKopiMottakere().get(0).rolle()).isEqualTo(ARBEIDSGIVER);
        assertThat(brevbestillingDto.getKopiMottakere().get(1).rolle()).isEqualTo(UTENLANDSK_TRYGDEMYNDIGHET);
    }

    @Test
    void fattVedtak_avslag_manglende_opplysninger_fatterVedtak() throws ValideringException {
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        FattVedtakRequest request = new FattVedtakRequest.Builder()
            .medBehandlingsresultat(AVSLAG_MANGLENDE_OPPL)
            .medVedtakstype(FØRSTEGANGSVEDTAK)
            .medFritekst("fritekst for beskrivelse avslag")
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();

        trygdeavtaleVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).endreStatus(behandlingCaptor.capture(), eq(IVERKSETTER_VEDTAK));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakTrygdeavtale(any(Behandling.class), eq(request));
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

    private FattVedtakRequest lagFattVedtakRequest(Vedtakstyper vedtakstype, String nyVurderingBakgrunn) {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(vedtakstype)
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medEktefelleFritekst("Ektefelle omfattet")
            .medBarnFritekst("Barn omfattet")
            .medKopiMottakere(List.of(
                new KopiMottakerDto(ARBEIDSGIVER, "987654321", null, null),
                new KopiMottakerDto(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "GB:UK010")
            ))
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .medNyVurderingBakgrunn(nyVurderingBakgrunn)
            .build();
    }

    private Behandling lagBehandling() {
        var behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(lagFagsak());
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger());
        return behandling;
    }

    private MottatteOpplysninger lagMottatteOpplysninger() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.soeknadsland.landkoder = List.of(Land_iso2.GB.getKode());
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);
        return mottatteOpplysninger;
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
