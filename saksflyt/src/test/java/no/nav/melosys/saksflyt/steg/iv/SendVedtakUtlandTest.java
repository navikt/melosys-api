package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.sed.SendVedtakUtland;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.hendelser.A1BestiltHendelse;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendVedtakUtlandTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private SedSomBrevService sedSomBrevService;
    @Mock
    private UtpekingService utpekingService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SendVedtakUtland sendVedtakUtland;

    private Prosessinstans prosessinstans;
    private Lovvalgsperiode lovvalgsperiode;
    private final Behandling behandling = new Behandling();

    @Captor
    private ArgumentCaptor<Brevbestilling> brevbestillingArgumentCaptor;

    private static final long BEHANDLING_ID = 1L;
    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @BeforeEach
    public void setUp() throws Exception {

        behandling.setId(BEHANDLING_ID);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

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

        sendVedtakUtland = new SendVedtakUtland(eessiService, behandlingService, behandlingsresultatService, brevBestiller, sedSomBrevService, utpekingService, applicationEventPublisher);
    }

    @Test
    void utfør_artikkel12Suksessfull_statusErOppdaterResultat() throws Exception {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), isNull(), isNull());
    }

    @Test
    void utfør_ingenInstitusjonEessiKlar_senderBrev() throws Exception {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());
        sendVedtakUtland.utfør(prosessinstans);
        verify(brevBestiller).bestill(brevbestillingArgumentCaptor.capture());
        assertThat(brevbestillingArgumentCaptor.getValue().getMottakere()).contains(Mottaker.av(Aktoersroller.MYNDIGHET));
        assertThat(brevbestillingArgumentCaptor.getValue().getDokumentType()).isEqualTo(Produserbaredokumenter.ATTEST_A1);
    }

    @Test
    void utfør_ForArtikkel11Suksessfull_statusErOppdaterResultat() throws Exception {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(MOTTAKER_INSTITUSJON));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(anyLong(), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), isNull(), isNull());
    }

    @SuppressWarnings("unchecked")
    @Test
    void utfør_utenOppgittMottakerinstitusjon_forventHenterMottakerinstitusjonFraTidligereBuc() throws MelosysException {
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
    void utfør_utpekAnnetLandUtenEessiMottakere_lagerBrev() throws MelosysException {
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        behandlingsresultat.setId(BEHANDLING_ID);
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
    }

    @Test
    void utfør_norgeErUtpektElektronisk_senderA012() throws MelosysException {
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Hei");
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        sendVedtakUtland.utfør(prosessinstans);

        verify(eessiService).sendGodkjenningArbeidFlereLand(eq(behandling.getId()), eq("Hei"));
    }

    @Test
    void utfør_sendA1_forventHendelse() throws MelosysException {
        sendVedtakUtland.utfør(prosessinstans);

        verify(brevBestiller).bestill(any(Brevbestilling.class));
        verify(applicationEventPublisher).publishEvent(any(A1BestiltHendelse.class));
    }
}