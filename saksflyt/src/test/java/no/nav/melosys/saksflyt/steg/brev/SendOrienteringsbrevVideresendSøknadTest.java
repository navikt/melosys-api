package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendOrienteringsbrevVideresendSøknadTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BrevBestiller brevBestiller;

    private SendOrienteringsbrevVideresendSøknad steg;
    private Behandling behandling;
    private Prosessinstans prosessinstans;

    @Captor
    private ArgumentCaptor<DoksysBrevbestilling> captor;


    @BeforeEach
    public void setup() {
        steg = new SendOrienteringsbrevVideresendSøknad(behandlingService , brevBestiller);

        behandling = new Behandling();
        behandling.setId(1L);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
    }

    @Test
    public void utfør_brevbestilling_harRiktigBrevTypeOgMottaker() {
        steg.utfør(prosessinstans);
        verify(brevBestiller).bestill(captor.capture());
        DoksysBrevbestilling brevbestilling = captor.getValue();
        assertThat(brevbestilling.getProduserbartdokument()).isEqualTo(Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD);
        assertThat(brevbestilling.getMottakere().stream().map(Mottaker::getRolle)).containsExactly(Aktoersroller.BRUKER);
        assertThat(brevbestilling.getBehandling()).isEqualTo(behandling);
    }
}
