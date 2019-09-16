package no.nav.melosys.saksflyt.steg.iv;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
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

    @Captor
    private ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        Institusjon institusjon1 = new Institusjon("AX:XOPB", "Ikke eksisterende", "AX");
        Institusjon institusjon2 = new Institusjon("YZ:123", "???", "YZ");
        List<Institusjon> institusjoner = Arrays.asList(institusjon1, institusjon2);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Landkoder.AX));
        when(eessiService.hentEessiMottakerinstitusjoner(anyString())).thenReturn(institusjoner);

        sendVedtakUtland = new SendVedtakUtland(eessiService, behandlingService, behandlingsresultatService, brevBestiller, landvelgerService);
    }

    @Test
    public void utførSteg_artikkel12Suksessfull_statusErAvgiftsoppgave() throws Exception{
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(BucType.LA_BUC_04));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførSteg_artikkel13Suksessfull_statusErAvgiftsoppgave() throws Exception{
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(BucType.LA_BUC_02));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførSteg_ingenInstitusjonEessiKlar_senderBrev() throws Exception{
        when(eessiService.hentEessiMottakerinstitusjoner(anyString())).thenReturn(Collections.emptyList());
        sendVedtakUtland.utfør(prosessinstans);
        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.av(Aktoersroller.MYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.ATTEST_A1);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utførStegForArtikkel11_suksessfull_statusErAvsluttBehandling() throws Exception {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(BucType.LA_BUC_05));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }
}