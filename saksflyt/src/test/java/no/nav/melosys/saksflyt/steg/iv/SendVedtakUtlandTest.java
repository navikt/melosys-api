package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
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
public class SendVedtakUtlandTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private LandvelgerService landvelgerService;

    private SendVedtakUtland sendVedtakUtland;

    private Prosessinstans prosessinstans;
    private Lovvalgsperiode lovvalgsperiode;
    private Behandling behandling = new Behandling();

    @Captor
    private ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @Before
    public void setUp() throws Exception {

        behandling.setId(BEHANDLING_ID);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLING_ID);
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Landkoder.AX));

        sendVedtakUtland = new SendVedtakUtland(eessiService, behandlingService, behandlingsresultatService, brevBestiller, landvelgerService);
    }

    @Test
    public void utførSteg_artikkel12Suksessfull_statusErAvgiftsoppgave() throws Exception {
        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), eq(Landkoder.AX.name()))).thenReturn(Boolean.TRUE);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), eq(null));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførSteg_artikkel13Suksessfull_statusErAvgiftsoppgave() throws Exception {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_02.name()), eq(Landkoder.AX.name()))).thenReturn(Boolean.TRUE);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_02), eq(null));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførSteg_ingenInstitusjonEessiKlar_senderBrev() throws Exception {
        sendVedtakUtland.utfør(prosessinstans);
        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.av(Aktoersroller.MYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.ATTEST_A1);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførStegForArtikkel11_suksessfull_statusErAvsluttBehandling() throws Exception {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_05.name()), eq(Landkoder.AX.name()))).thenReturn(Boolean.TRUE);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), eq(null));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void utførSteg_utenOppgittMottakerinstitusjon_forventHenterMottakerinstitusjonFraTidligereBuc() throws MelosysException {
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Landkoder.SE));
        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), eq(Landkoder.SE.name()))).thenReturn(Boolean.TRUE);
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(""));

        Aktoer myndighet = new Aktoer();
        myndighet.setInstitusjonId(MOTTAKER_INSTITUSJON);
        myndighet.setRolle(Aktoersroller.MYNDIGHET);

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(myndighet));
        fagsak.setGsakSaksnummer(1L);
        behandling.setFagsak(fagsak);

        when(eessiService.hentMottakerinstitusjonFraBuc(any(Fagsak.class), any(BucType.class))).thenReturn(MOTTAKER_INSTITUSJON);

        sendVedtakUtland.utfør(prosessinstans);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(eessiService).opprettOgSendSed(anyLong(), captor.capture(), any(), any());

        assertThat(captor.getValue()).containsExactly(MOTTAKER_INSTITUSJON);
    }
}