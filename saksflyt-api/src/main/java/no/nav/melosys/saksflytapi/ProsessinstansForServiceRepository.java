package no.nav.melosys.saksflytapi;

import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProsessinstansForServiceRepository extends JpaRepository<Prosessinstans, UUID> {
    Optional<Prosessinstans> findByBehandling_IdAndStatusNot(long id, ProsessStatus prosessStatus);

    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);
}
