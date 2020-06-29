package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
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
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private SedSomBrevService sedSomBrevService;

    private SendVedtakUtland sendVedtakUtland;

    private Prosessinstans prosessinstans;
    private Lovvalgsperiode lovvalgsperiode;
    private final Behandling behandling = new Behandling();

    @Captor
    private ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @Before
    public void setUp() throws Exception {

        behandling.setId(BEHANDLING_ID);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
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

        sendVedtakUtland = new SendVedtakUtland(eessiService, behandlingService, behandlingsresultatService,
            brevBestiller, saksopplysningerService, sedSomBrevService);
    }

    @Test
    public void utførSteg_artikkel12Suksessfull_statusErOppdaterResultat() throws Exception {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), isNull(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_RESULTAT);
    }

    @Test
    public void utførSteg_artikkel13Suksessfull_statusErOppdaterResultat() throws Exception {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_02), isNull(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_RESULTAT);
    }

    @Test
    public void utførSteg_ingenInstitusjonEessiKlar_senderBrev() throws Exception {
        sendVedtakUtland.utfør(prosessinstans);
        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.av(Aktoersroller.MYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.ATTEST_A1);
    }

    @Test
    public void utførStegForArtikkel11Suksessfull_statusErOppdaterResultat() throws Exception {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), isNull(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_RESULTAT);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void utførSteg_utenOppgittMottakerinstitusjon_forventHenterMottakerinstitusjonFraTidligereBuc() throws MelosysException {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));

        Aktoer myndighet = new Aktoer();
        myndighet.setInstitusjonId(MOTTAKER_INSTITUSJON);
        myndighet.setRolle(Aktoersroller.MYNDIGHET);

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(myndighet));
        fagsak.setGsakSaksnummer(1L);
        behandling.setFagsak(fagsak);

        sendVedtakUtland.utfør(prosessinstans);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(eessiService).opprettOgSendSed(anyLong(), captor.capture(), any(), any(), isNull());

        assertThat(captor.getValue()).containsExactly(MOTTAKER_INSTITUSJON);
    }

    @Test
    public void utførSteg_utpekAnnetLandUtenEessiMottakere_lagerBrev() throws MelosysException {
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2);
        lovvalgsperiode.setLovvalgsland(Landkoder.AT);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, Landkoder.AT);
        when(sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(eq(SedType.A003), any(), any()))
            .thenReturn("journalpostID");
        sendVedtakUtland.utfør(prosessinstans);
        verify(sedSomBrevService)
            .lagJournalpostForSendingAvSedSomBrev(eq(SedType.A003), eq(Landkoder.AT), eq(behandling));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.UL_DISTRIBUER_JOURNALPOST);
    }

    @Test
    public void utførSteg_norgeErUtpektElektronisk_senderA012() throws MelosysException {
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Hei");
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setErElektronisk(true);
        when(saksopplysningerService.hentSedOpplysninger(eq(behandling.getId()))).thenReturn(sedDokument);

        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService).sendGodkjenningArbeidFlereLand(eq(behandling.getId()), eq("Hei"));
    }
}