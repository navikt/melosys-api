package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendOrienteringAnmodningUnntakTest {

    @Mock
    private BrevBestiller brevBestiller;

    @Mock
    private BehandlingService behandlingService;

    private Prosessinstans prosessinstans;
    private SendOrienteringAnmodningUnntak sendOrienteringAnmodningUnntak;

    private static final String SAKSBEHANDLER = "Z121212";

    @BeforeEach
    public void setUp() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setId(1L);

        when(behandlingService.hentBehandling(eq(behandling.getId()))).thenReturn(behandling);

        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.ANMODNING_OM_UNNTAK.getKode());

        sendOrienteringAnmodningUnntak = new SendOrienteringAnmodningUnntak(brevBestiller, behandlingService);
    }

    @Test
    void utfoerSteg() {
        sendOrienteringAnmodningUnntak.utfør(prosessinstans);
        verify(brevBestiller).bestill(eq(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK), eq(SAKSBEHANDLER), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));
    }
}
