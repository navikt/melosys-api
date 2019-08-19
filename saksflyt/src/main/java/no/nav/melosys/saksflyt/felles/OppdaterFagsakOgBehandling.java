package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterFagsakOgBehandling {

    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppdaterFagsakOgBehandling(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public void oppdaterFagsakOgBehandlingStatuser(Behandling behandling, Saksstatuser saksstatus, Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(saksstatus);
        fagsakRepository.save(fagsak);
        behandling.setStatus(behandlingsstatus);
        behandlingRepository.save(behandling);
    }
}
