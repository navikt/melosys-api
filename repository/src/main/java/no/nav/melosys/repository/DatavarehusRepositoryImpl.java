package no.nav.melosys.repository;

import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.FagsakDvh;
import org.springframework.stereotype.Component;

@Component
public class DatavarehusRepositoryImpl implements DatavarehusRepository {

    private final FagsakDvhRepository fagsakDvhRepository;

    private final BehandlingDvhRepository behandlingDvhRepository;

    public DatavarehusRepositoryImpl(FagsakDvhRepository fagsakDvhRepository, BehandlingDvhRepository behandlingDvhRepository) {
        this.fagsakDvhRepository = fagsakDvhRepository;
        this.behandlingDvhRepository = behandlingDvhRepository;
    }

    @Override
    public FagsakDvh lagre(FagsakDvh fagsakDvh) {
        return fagsakDvhRepository.save(fagsakDvh);
    }

    @Override
    public BehandlingDvh lagre(BehandlingDvh behandlingDvh) {
        return behandlingDvhRepository.save(behandlingDvh);
    }
}
