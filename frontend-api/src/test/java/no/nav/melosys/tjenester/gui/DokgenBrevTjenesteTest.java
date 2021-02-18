package no.nav.melosys.tjenester.gui;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevmalInnholdService;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenBrevTjenesteTest {

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;

    private DokgenBrevTjeneste dokgenBrevTjeneste;

    @BeforeEach
    void init() {
        BrevmalInnholdService brevmalInnholdService = new BrevmalInnholdService(mockBehandlingService, mockAvklarteVirksomheterService);
        dokgenBrevTjeneste = new DokgenBrevTjeneste(brevmalInnholdService);
    }

    @Test
    void skalReturnereTilgjengeligeBrevmaler() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());

        List<BrevmalDto> brevmaler = dokgenBrevTjeneste.hentTilgjengeligeMaler(123L);
        assertEquals(2, brevmaler.size());
    }

}