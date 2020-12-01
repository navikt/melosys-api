package no.nav.melosys.saksflyt.impl;

import java.util.Collections;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.hendelser.A1BestiltHendelse;
import no.nav.melosys.service.hendelser.FeiletHendelse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeiletHendelseHandlerTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private StegBehandler mangelBrevStebehandler;
    @Mock
    private BehandlingService behandlingService;

    private FeiletHendelseHandler feiletHendelseHandler;

    @BeforeEach
    public void setup() {
        when(mangelBrevStebehandler.inngangsSteg()).thenReturn(ProsessSteg.MANGELBREV);
        feiletHendelseHandler = new ProsessinstansBehandlerImpl(Collections.singleton(mangelBrevStebehandler), prosessinstansRepository, behandlingService);
    }

    @Test
    void behandleFeiletHendelse_forventNoe() throws IkkeFunnetException {
        A1BestiltHendelse a1BestiltHendelse = new A1BestiltHendelse(this, 1L);
        FeiletHendelse feiletHendelse = new FeiletHendelse(this, new FunksjonellException("noe gikk galt"), a1BestiltHendelse);

        feiletHendelseHandler.behandleFeiletHendelse(feiletHendelse);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(1L);
    }
}