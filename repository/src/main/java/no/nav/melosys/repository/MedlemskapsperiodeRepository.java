package no.nav.melosys.repository;

import no.nav.melosys.domain.Medlemskapsperiode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MedlemskapsperiodeRepository extends JpaRepository<Medlemskapsperiode, Long> {

    Optional<Medlemskapsperiode> findByMedlPeriodeID(Long medlPeriodeID);
}
