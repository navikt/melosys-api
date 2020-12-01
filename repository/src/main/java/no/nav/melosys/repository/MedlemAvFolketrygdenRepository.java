package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedlemAvFolketrygdenRepository extends JpaRepository<MedlemAvFolketrygden, Long> {
    Optional<MedlemAvFolketrygden> findByBehandlingsresultatId(long behandlingsresultatID);
}
