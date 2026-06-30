package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.SkjemaSakMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkjemaSakMappingRepository extends JpaRepository<SkjemaSakMapping, UUID> {

    List<SkjemaSakMapping> findBySkjemaIdIn(Collection<UUID> skjemaIds);

    Optional<SkjemaSakMapping> findBySkjemaId(UUID skjemaId);

    List<SkjemaSakMapping> findByMottatteOpplysninger_Id(Long mottatteOpplysningerId);
}
