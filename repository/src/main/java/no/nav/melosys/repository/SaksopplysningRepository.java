package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaksopplysningRepository extends JpaRepository<Saksopplysning, Long> {

    boolean existsByBehandling_IdAndType(long behandlingID, SaksopplysningType type);

    Optional<Saksopplysning> findByBehandling_IdAndType(long behandlingID, SaksopplysningType type);

}
