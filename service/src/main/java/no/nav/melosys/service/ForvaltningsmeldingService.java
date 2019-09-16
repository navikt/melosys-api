package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForvaltningsmeldingService {

    private final ProsessinstansService prosessinstansService;

    private final BehandlingService behandlingService;

    @Autowired
    public ForvaltningsmeldingService(ProsessinstansService prosessinstansService, 
            BehandlingService behandlingService) {
        this.prosessinstansService = prosessinstansService;
        this.behandlingService = behandlingService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void sendForvaltningsmelding(long behandlingId) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        prosessinstansService.opprettProsessinstansForvaltningsmeldingSend(behandling);
    }
}
