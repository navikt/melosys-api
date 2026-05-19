package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.PeriodeKilde;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedlemskapsperiodeRepository extends JpaRepository<Medlemskapsperiode, Long> {
    List<Medlemskapsperiode> findByBehandlingsresultatIdAndKilde(long behandlingsresultatId, PeriodeKilde kilde);
}
