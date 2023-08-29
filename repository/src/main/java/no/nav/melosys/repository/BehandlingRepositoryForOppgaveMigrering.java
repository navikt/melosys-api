package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SakOgBehandlingDTO;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface BehandlingRepositoryForOppgaveMigrering extends CrudRepository<Behandling, Long> {
    @EntityGraph(attributePaths = "saksopplysninger")
    @Nullable
    Behandling findWithSaksopplysningerById(Long behandlingID);

    @Query("SELECT new no.nav.melosys.domain.SakOgBehandlingDTO(" +
            "e.fagsak.saksnummer, e.id, e.initierendeJournalpostId, e.fagsak.type, e.fagsak.tema, e.type, e.tema, e.status, br.type) " +
            "FROM Behandling e, Behandlingsresultat br WHERE e.status NOT IN (:excludedStatuses) and br.behandling.id = e.id")
    Collection<SakOgBehandlingDTO> finnSaksOgBehandlingTyperOgTema(@Param("excludedStatuses") List<Behandlingsstatus> excludedStatuses);

    @Query("SELECT new no.nav.melosys.domain.SakOgBehandlingDTO(" +
            "e.fagsak.saksnummer, e.id, e.initierendeJournalpostId, e.fagsak.type, e.fagsak.tema, e.type, e.tema, e.status, br.type) " +
            "FROM Behandling e, Behandlingsresultat br WHERE e.status NOT IN (:excludedStatuses) " +
             "and (e.fagsak.registrertAv = :bruker or e.fagsak.endretAv = :bruker and br.behandling.id = e.id)")
    Collection<SakOgBehandlingDTO> finnSakerRegistrertAv(
            @Param("bruker") String bruker,
            @Param("excludedStatuses") List<Behandlingsstatus> excludedStatuses
            );

    @Query("SELECT new no.nav.melosys.domain.SakOgBehandlingDTO(" +
            "e.fagsak.saksnummer, e.id, e.initierendeJournalpostId, e.fagsak.type, e.fagsak.tema, e.type, e.tema, e.status, br.type) " +
            "FROM Behandling e, Behandlingsresultat br WHERE e.status NOT IN (:excludedStatuses) " +
             "and e.fagsak.saksnummer = :saksnummer and br.behandling.id = e.id")
    Collection<SakOgBehandlingDTO> finnSak(
            @Param("saksnummer") String saksnummer,
            @Param("excludedStatuses") List<Behandlingsstatus> excludedStatuses
            );
}
