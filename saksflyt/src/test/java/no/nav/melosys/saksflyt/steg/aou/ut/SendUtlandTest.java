package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendUtlandTest {
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

    private SendUtland sendUtland;

    private Prosessinstans prosessinstans;
    @Captor
    private ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITSJON = "SE:123";

    @Before
    public void setUp() {
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(BEHANDLING_ID);
        prosessinstans.getBehandling().setDokumentasjonSvarfristDato(Instant.now());

        sendUtland = new SendUtland(eessiService, brevBestiller, behandlingService, behandlingsresultatService, anmodningsperiodeService);
    }

    @Test
    public void utfør_artikkel16_verifiserStegFerdig() throws Exception {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITSJON));

        sendUtland.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_OPPGAVE);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITSJON)), eq(BucType.LA_BUC_01), eq(null));
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(eq(BEHANDLING_ID));
    }

    @Test
    public void utfør_ingenInstitusjonEessiKlar_senderBrev() throws Exception {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);

        sendUtland.utfør(prosessinstans);

        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.av(Aktoersroller.MYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.ANMODNING_UNNTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_OPPGAVE);
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(eq(BEHANDLING_ID));
    }

    @Test
    public void utfør_ingenBestemmelse_verifiserSedIkkeSendt() throws Exception {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(new Anmodningsperiode()));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(2L))).thenReturn(behandlingsresultat);
        prosessinstans.getBehandling().setId(2L);
        Instant nå = prosessinstans.getBehandling().getDokumentasjonSvarfristDato();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITSJON));

        sendUtland.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_OPPGAVE);
        assertThat(nå).isBefore(prosessinstans.getBehandling().getDokumentasjonSvarfristDato());
        verify(eessiService, never()).opprettOgSendSed(anyLong(), any(), any());
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(eq(prosessinstans.getBehandling().getId()));
    }

    private static Behandlingsresultat hentBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        behandlingsresultat.setBehandling(hentBehandling());
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now(), Landkoder.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);
        behandlingsresultat.setAnmodningsperioder(Sets.newHashSet(anmodningsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        return behandlingsresultat;
    }

    private static Behandling hentBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        return behandling;
    }
}