package no.nav.melosys.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProsessinstansRepository extends JpaRepository<Prosessinstans, UUID> {
    @Query("SELECT NEW no.nav.melosys.repository.ProsessinstansAntall(p.type, p.status, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.status <> no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG "
        + "AND p.type IN (?1) GROUP BY p.type, p.status")
    Collection<ProsessinstansAntall> antallAktiveOgFeiletPerTypeOgStatus(Collection<ProsessType> typer);

    Optional<Prosessinstans> findByBehandling_IdAndStatusIs(long id, ProsessStatus prosessStatus);

    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);

    Collection<Prosessinstans> findAllByStatus(ProsessStatus status);
}
