package no.nav.melosys.repository;

import java.time.Instant;
import java.util.Collection;

import no.nav.melosys.domain.VedtakMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;

public interface VedtakMetadataRepository extends JpaRepository<VedtakMetadata, Long> {
    @NativeQuery("select behandlingsresultat_id from vedtak_metadata v where v.registrert_dato >= ?1")
    Collection<Long> findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(Instant fom);
}
