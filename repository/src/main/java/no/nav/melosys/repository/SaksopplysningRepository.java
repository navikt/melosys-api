package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.data.repository.CrudRepository;

public interface SaksopplysningRepository extends CrudRepository<Saksopplysning, Long> {

    Optional<Saksopplysning> findByBehandlingAndType(Behandling behandling, SaksopplysningType type);

}
