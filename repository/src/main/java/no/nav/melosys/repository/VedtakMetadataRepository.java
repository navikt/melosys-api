package no.nav.melosys.repository;

import java.time.Instant;
import java.util.Collection;

import no.nav.melosys.domain.VedtakMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VedtakMetadataRepository extends JpaRepository<VedtakMetadata, Long> {
    @Query(
        value = "select behandlingsresultat_id from vedtak_metadata v where v.registrert_dato >= ?1",
        nativeQuery = true
    )
    Collection<Long> findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(Instant fom);
}
