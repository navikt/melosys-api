package no.nav.melosys.saksflyt.steg.vilkaar;

import java.time.LocalDate;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VurderInngangsvilkaarTest {
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private BehandlingService behandlingService;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    private final long behandlingID = 143;
    private final Behandling behandling = new Behandling();
    private final FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    public void setUp() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, behandlingService, unleash);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);

        unleash.enableAll();
    }

    @Test
    void utfoerSteg_funker() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        behandlingsgrunnlagData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("MEL-432");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            behandlingID,
            behandlingsgrunnlagData.soeknadsland.landkoder,
            false,
            behandlingsgrunnlagData.periode
        )).thenReturn(true);


        vurderInngangsvilkaar.utfør(prosessinstans);


        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_behandlingstemaBeslutningLovvalgAnnetLandToggleAv_vurdererIkkeInngangsvilkår() {
        unleash.disableAll();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_ikkeSakstypeEuEøs_vurdererIkkeInngangsvilkår() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.TRYGDEAVTALE);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_harIkkeFlyt_vurdererIkkeInngangsvilkår() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setType(Behandlingstyper.HENVENDELSE);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_kanIkkeResultereIVedtak_vurdererIkkeInngangsvilkår() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }
}
