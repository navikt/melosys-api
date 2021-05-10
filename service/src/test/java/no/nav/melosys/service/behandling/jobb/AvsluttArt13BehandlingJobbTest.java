package no.nav.melosys.service.behandling.jobb;


import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvsluttArt13BehandlingJobbTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private AvsluttArt13BehandlingService avsluttArt13BehandlingService;

    private AvsluttArt13BehandlingJobb avsluttArt13BehandlingJobb;

    @BeforeEach
    public void setup() {
        avsluttArt13BehandlingJobb = new AvsluttArt13BehandlingJobb(behandlingService, avsluttArt13BehandlingService);
    }

    @Test
    public void avsluttBehandlingArt13_femBehandlinger_serviceBlirKalt() throws FunksjonellException, TekniskException {

        Behandling b1 = new Behandling();
        b1.setId(111L);

        Behandling b2 = new Behandling();
        b2.setId(222L);

        when(behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)).thenReturn(List.of(b1, b2));

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13();
        verify(avsluttArt13BehandlingService).avsluttBehandlingHvisToMndPassert(b1.getId());
        verify(avsluttArt13BehandlingService).avsluttBehandlingHvisToMndPassert(b2.getId());
    }

}
