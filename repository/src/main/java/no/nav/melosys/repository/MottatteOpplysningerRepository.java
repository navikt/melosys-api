package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MottatteOpplysningerRepository extends JpaRepository<MottatteOpplysninger, Long> {
    void deleteByBehandling_Id(long behandlingId);

    Optional<MottatteOpplysninger> findByBehandling_Id(long behandlingId);

    List<MottatteOpplysninger> findByEksternReferanseID(String eksternReferanseID);
}
