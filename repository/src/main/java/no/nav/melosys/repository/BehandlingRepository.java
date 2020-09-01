package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;

public interface BehandlingRepository extends CrudRepository<Behandling, Long> {

    List<Behandling> findByStatusNot(Behandlingsstatus status);

    @Query("select b from Behandling b, Fagsak f where b.fagsak.saksnummer = f.saksnummer and f.gsakSaksnummer = ?1") //$NON-NLS-1$
    List<Behandling> findBySaksnummer(String saksnummer);

    @EntityGraph(attributePaths = "saksopplysninger")
    @Nullable
    Behandling findWithSaksopplysningerById(Long behandlingID);

    Collection<Behandling> findAllByStatus(Behandlingsstatus behandlingsstatus);

    @Query("SELECT NEW no.nav.melosys.repository.BehandlingStatistikk(b.tema, COUNT(b)) FROM Behandling b "
        + "WHERE b.status NOT IN (no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET, " +
        "no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING) GROUP BY b.tema")
    List<BehandlingStatistikk> antallÅpneBehandlingerPerBehandlingstema();
}
