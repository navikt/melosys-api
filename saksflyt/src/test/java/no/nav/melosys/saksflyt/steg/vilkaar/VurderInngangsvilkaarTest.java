package no.nav.melosys.saksflyt.steg.vilkaar;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
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
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    private final long behandlingID = 143;
    private final Behandling behandling = new Behandling();

    @BeforeEach
    public void setUp() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, behandlingService, saksbehandlingRegler);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
    }

    @Test
    void utfoerSteg_funker() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        mottatteOpplysningerData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(mottatteOpplysningerData);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("MEL-432");
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            behandlingID,
            mottatteOpplysningerData.soeknadsland.landkoder,
            false,
            mottatteOpplysningerData.periode
        )).thenReturn(true);


        vurderInngangsvilkaar.utfør(prosessinstans);


        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfoerSteg_finnerIkkeLandOgPeriode_vurdererIkkeInngangsvilkår() {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        var mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.periode = new Periode();
        mottatteOpplysningerData.soeknadsland = new Soeknadsland();
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(mottatteOpplysningerData);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);


        vurderInngangsvilkaar.utfør(prosessinstans);


        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }

    @Test
    void utfør_behandlingstemaBeslutningLovvalgAnnetLandToggleAv_vurdererIkkeInngangsvilkår() {
        when(saksbehandlingRegler.harIngenFlyt(any())).thenReturn(false);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

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
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
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
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        vurderInngangsvilkaar.utfør(prosessinstans);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }
}
