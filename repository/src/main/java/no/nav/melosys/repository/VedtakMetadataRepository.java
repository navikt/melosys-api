package no.nav.melosys.repository;

import java.time.Instant;
import java.util.Collection;

import no.nav.melosys.domain.VedtakMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VedtakMetadataRepository extends JpaRepository<VedtakMetadata, Long> {
    Collection<Long> findBehandlingIdByRegistrertDatoIsGreaterThanEqual(Instant fom);
}
