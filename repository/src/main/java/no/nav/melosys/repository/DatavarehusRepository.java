package no.nav.melosys.repository;

import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.FagsakDvh;

public interface DatavarehusRepository {

    FagsakDvh lagre(FagsakDvh fagsakDvh);

    BehandlingDvh lagre(BehandlingDvh behandlingDvh);
}