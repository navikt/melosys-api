package no.nav.melosys.saksflyt.steg.vilkaar;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
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
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
    }

    @Test
    void utfoerSteg_funker() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1L));
        mottatteOpplysningerData.soeknadsland.landkoder = List.of(Landkoder.NO.getKode(), Landkoder.SE.getKode());

        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(mottatteOpplysningerData);
        behandling.setFagsak(new Fagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(inngangsvilkaarService.skalVurdereInngangsvilkår(any())).thenReturn(true);
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
    void utfoerSteg_skalIkkeVurdereInngangsvilkår_vurdererIkkeInngangsvilkår() {
        when(inngangsvilkaarService.skalVurdereInngangsvilkår(any())).thenReturn(false);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        behandling.setFagsak(new Fagsak());


        vurderInngangsvilkaar.utfør(prosessinstans);


        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any());
    }
}
