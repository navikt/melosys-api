package no.nav.melosys.repository;

import java.util.Collection;

import no.nav.melosys.domain.Medlemskapsperiode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedlemskapsperiodeRepository extends JpaRepository<Medlemskapsperiode, Long> {
    Collection<Medlemskapsperiode> findByBehandlingsresultatId(long id);
}
