package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtkastBrevRepository extends JpaRepository<UtkastBrev, Long> {

    List<UtkastBrev> findAllByBehandlingIDOrderByLagringsdatoDesc(long behandlingID);

}
