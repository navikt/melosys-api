package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaksopplysningRepository extends JpaRepository<Saksopplysning, Long> {

    Optional<Saksopplysning> findByBehandlingAndType(Behandling behandling, SaksopplysningType type);

    void deleteAllByBehandling(Behandling behandling);
}
