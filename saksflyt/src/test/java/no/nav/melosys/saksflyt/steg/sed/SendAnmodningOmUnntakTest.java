package no.nav.melosys.saksflyt.steg.sed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.YTTERLIGERE_INFO_SED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendAnmodningOmUnntakTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;

    private SendAnmodningOmUnntak sendAnmodningOmUnntak;

    private Prosessinstans prosessinstans;
    @Captor
    private ArgumentCaptor<DoksysBrevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITSJON = "SE:123";

    @BeforeEach
    void setUp() {
        prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(lagBehandling())
            .build();

        sendAnmodningOmUnntak = new SendAnmodningOmUnntak(eessiService, brevBestiller, behandlingService,
            behandlingsresultatService, anmodningsperiodeService);
    }

    @Test
    void utfør_artikkel16_sendSedMedVedlegg() {
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITSJON))
            .build();
        final var dokumentReferanse = new DokumentReferanse("", "");
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.VEDLEGG_SED, Set.of(dokumentReferanse))
            .build();
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);
        final Vedlegg forventetVedlegg = new Vedlegg(new byte[0], "tittel");
        when(eessiService.lagEessiVedlegg(any(), any())).thenReturn(Set.of(forventetVedlegg));

        sendAnmodningOmUnntak.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITSJON)), eq(BucType.LA_BUC_01),
            argThat(collection -> collection.contains(forventetVedlegg)), isNull());
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID);
    }

    @Test
    void utfør_artikkel18_1_sendSed() {
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITSJON))
            .build();
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        behandlingsresultat.getAnmodningsperioder().forEach(periode -> periode.setBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        sendAnmodningOmUnntak.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITSJON)), eq(BucType.LA_BUC_01), any(), isNull());
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID);
    }

    @Test
    void utfør_ingenInstitusjonEessiKlar_senderBrev() {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);
        prosessinstans = prosessinstans.toBuilder()
            .medData(YTTERLIGERE_INFO_SED, "Mer info")
            .build();

        sendAnmodningOmUnntak.utfør(prosessinstans);

        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(Produserbaredokumenter.ANMODNING_UNNTAK);
        assertThat(brevbestillingArgumentCaptor.getValue().getYtterligereInformasjon()).isEqualTo("Mer info");
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID);
    }

    @Test
    void utfør_ingenBestemmelse_verifiserSedIkkeSendt() {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(new Anmodningsperiode()));
        when(behandlingsresultatService.hentBehandlingsresultat(2L)).thenReturn(behandlingsresultat);
        prosessinstans.getBehandling().setId(2L);
        Instant nå = prosessinstans.getBehandling().getDokumentasjonSvarfristDato();
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITSJON))
            .build();
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.YTTERLIGERE_INFO_SED, "fritekst")
            .build();

        sendAnmodningOmUnntak.utfør(prosessinstans);

        assertThat(nå).isBefore(prosessinstans.getBehandling().getDokumentasjonSvarfristDato());
        verify(eessiService, never()).opprettOgSendSed(anyLong(), anyList(), eq(BucType.LA_BUC_01), isNull(), eq("fritekst"));
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(prosessinstans.getBehandling().getId());
    }

    private static Behandlingsresultat hentBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        behandlingsresultat.setBehandling(lagBehandling());
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now(), Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);
        behandlingsresultat.setAnmodningsperioder(Sets.newHashSet(anmodningsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        return behandlingsresultat;
    }

    private static Behandling lagBehandling() {
        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        return BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .medId(BEHANDLING_ID)
            .medDokumentasjonSvarfristDato(Instant.now())
            .build();
    }
}
