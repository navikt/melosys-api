package no.nav.melosys.saksflyt.steg.vilkaar;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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

    @BeforeEach
    public void setUp() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, behandlingService);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        when(behandlingService.hentBehandlingMedSaksopplysninger(eq(behandlingID))).thenReturn(behandling);
    }

    @Test
    void utfoerSteg_funker() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        behandlingsgrunnlagData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("MEL-432");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            eq(behandlingID),
            eq(behandlingsgrunnlagData.soeknadsland.landkoder),
            eq(false),
            eq(behandlingsgrunnlagData.periode)
        )).thenReturn(true);

        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_behandlingstemaBeslutningLovvalgAnnetLand_vurdererIkkeInngangsvilkår() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_sakstypeFtrl_vurdererIkkeInngangsvilkår() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.FTRL);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }
}
