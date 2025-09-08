package no.nav.melosys.repository;

import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;

public interface BehandlingRepository extends CrudRepository<Behandling, Long> {

    @EntityGraph(attributePaths = "saksopplysninger")
    @Nullable
    Behandling findWithSaksopplysningerById(Long behandlingID);

    Collection<Long> findIdsByStatus(Behandlingsstatus behandlingsstatus);

}
