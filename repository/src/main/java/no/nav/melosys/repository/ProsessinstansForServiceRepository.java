package no.nav.melosys.repository;

import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProsessinstansForServiceRepository extends JpaRepository<Prosessinstans, UUID> {
    Optional<Prosessinstans> findByBehandling_IdAndStatusIs(long id, ProsessStatus prosessStatus);

    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);
}
