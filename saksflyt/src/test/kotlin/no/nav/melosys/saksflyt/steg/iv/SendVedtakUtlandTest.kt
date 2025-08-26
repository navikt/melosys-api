package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.saksflyt.steg.sed.SendVedtakUtland;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendVedtakUtlandTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private SedSomBrevService sedSomBrevService;
    @Mock
    private UtpekingService utpekingService;
    @Mock
    private ProsessinstansService prosessinstansService;

    private SendVedtakUtland sendVedtakUtland;

    private Prosessinstans prosessinstans;
    private Lovvalgsperiode lovvalgsperiode;
    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;
    private Fagsak fagsak;

    private final FakeUnleash fakeUnleash = new FakeUnleash();
    @Captor
    private ArgumentCaptor<DoksysBrevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @BeforeEach
    public void setUp() {
        fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medFagsak(fagsak)
            .build();

        prosessinstans = ProsessinstansTestFactory.builderWithDefaults()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .build();

        behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        sendVedtakUtland = new SendVedtakUtland(eessiService, behandlingsresultatService, sedSomBrevService, utpekingService, prosessinstansService, fakeUnleash);
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setVedtakMetadata(new VedtakMetadata());
        Set<Avklartefakta> avklartefakta = Set.of(new Avklartefakta());
        behandlingsresultat.setAvklartefakta(avklartefakta);
        return behandlingsresultat;
    }

    @Test
    void utfør_artikkel12Suksessfull_statusErOppdaterResultat() {
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON))
            .build();
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), eq(Collections.emptySet()), isNull());
    }

    @Test
    void utfør_ingenInstitusjonEessiKlar_senderBrev() {
        when(behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(behandling.getId()))
            .thenReturn(lagBehandlingsresultat());

        sendVedtakUtland.utfør(prosessinstans);

        verify(prosessinstansService).opprettProsessinstansSendBrev(eq(behandling), brevbestillingArgumentCaptor.capture(), eq(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)));
        assertThat(brevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(Produserbaredokumenter.ATTEST_A1);
    }

    @Test
    void utfør_ForArtikkel11Suksessfull_statusErOppdaterResultat() {
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON))
            .build();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), eq(Collections.emptySet()), isNull());
    }

    @Test
    void utfør_utenOppgittMottakerinstitusjon_forventHenterMottakerinstitusjonFraTidligereBuc() {
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON))
            .build();

        behandling.setFagsak(fagsak);

        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), any(), any(), isNull());
    }

    @Test
    void utfør_utpekAnnetLandUtenEessiMottakere_lagerBrev() {
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        behandlingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        behandlingsresultat.setId(BEHANDLING_ID);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2);
        lovvalgsperiode.setLovvalgsland(Land_iso2.AT);
        UUID prosessinstansUuid = UUID.randomUUID();
        prosessinstans = prosessinstans.toBuilder()
            .medId(prosessinstansUuid)
            .medData(ProsessDataKey.UTPEKT_LAND, Landkoder.AT)
            .build();
        when(sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(eq(SedType.A003), any(), any(), eq(prosessinstansUuid.toString())))
            .thenReturn("journalpostID");
        sendVedtakUtland.utfør(prosessinstans);
        verify(sedSomBrevService)
            .lagJournalpostForSendingAvSedSomBrev(SedType.A003, Land_iso2.AT, behandling, prosessinstansUuid.toString());
    }

    @Test
    void utfør_vedtakEtterArt16HarTilknyttetLaBuc01_lukkerBuc() {
        final var rinaSaksnummer = "5453";
        behandlingsresultat.hentLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        behandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);

        when(eessiService.hentTilknyttedeBucer(fagsak.getGsakSaksnummer(), Collections.emptyList()))
            .thenReturn(List.of(new BucInformasjon(rinaSaksnummer, true, BucType.LA_BUC_01.name(), LocalDate.now(), Set.of(), Collections.emptyList())));

        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService).lukkBuc(rinaSaksnummer);
    }

    @Test
    void utfør_norgeErUtpektElektroniskBucÅpen_senderA012() {
        when(eessiService.hentTilknyttedeBucer(eq(fagsak.getGsakSaksnummer()), any()))
            .thenReturn(List.of(new BucInformasjon("5453", true, BucType.LA_BUC_02.name(), LocalDate.now(), Set.of(), Collections.emptyList())));

        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Hei")
            .build();
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService).sendGodkjenningArbeidFlereLand(behandling.getId(), "Hei");
    }

    @Test
    void utfør_norgeErUtpektElektroniskBukLukket_senderIkkeA012() {
        when(eessiService.hentTilknyttedeBucer(eq(fagsak.getGsakSaksnummer()), any()))
            .thenReturn(List.of(new BucInformasjon("5453", false, BucType.LA_BUC_02.name(), LocalDate.now(), Set.of(), Collections.emptyList())));
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);

        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), anyString());
    }
}
