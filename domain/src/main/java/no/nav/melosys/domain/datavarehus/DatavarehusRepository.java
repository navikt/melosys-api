package no.nav.melosys.domain.datavarehus;

public interface DatavarehusRepository {

    FagsakDvh lagre(FagsakDvh fagsakDvh);

    BehandlingDvh lagre(BehandlingDvh behandlingDvh);
}