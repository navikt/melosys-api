package no.nav.melosys.repository;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SakOgBehandlingDTO;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

public interface BehandlingRepository extends CrudRepository<Behandling, Long> {

    @EntityGraph(attributePaths = "saksopplysninger")
    @Nullable
    Behandling findWithSaksopplysningerById(Long behandlingID);

    Collection<Behandling> findAllByStatus(Behandlingsstatus behandlingsstatus);

    @Query("SELECT new no.nav.melosys.domain.SakOgBehandlingDTO(" +
            "e.fagsak.saksnummer, e.fagsak.type, e.fagsak.tema, e.type, e.tema, e.status) " +
            "FROM Behandling e WHERE e.status NOT IN (:excludedStatuses)")
    Collection<SakOgBehandlingDTO> findSaksOgBehandlingTyperOgTeam(@Param("excludedStatuses") List<Behandlingsstatus> excludedStatuses);
}
