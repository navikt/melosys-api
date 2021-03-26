package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaksopplysningRepository extends JpaRepository<Saksopplysning, Long> {

    Optional<Saksopplysning> findByBehandlingAndType(Behandling behandling, SaksopplysningType type);

    Optional<Saksopplysning> findByBehandling_IdAndType(long behandlingID, SaksopplysningType type);

    void deleteAllByBehandling(Behandling behandling);
}
