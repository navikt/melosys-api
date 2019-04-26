package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterFagsakOgBehandlingFelles {

    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppdaterFagsakOgBehandlingFelles(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public void avsluttFagsakOgBehandling(Behandling behandling, Saksstatuser saksstatus, Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(saksstatus);
        fagsakRepository.save(fagsak);
        behandling.setStatus(behandlingsstatus);
        behandlingRepository.save(behandling);
    }
}
