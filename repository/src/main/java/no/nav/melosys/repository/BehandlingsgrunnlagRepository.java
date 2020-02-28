package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehandlingsgrunnlagRepository extends JpaRepository<Behandlingsgrunnlag, Long> {
    Optional<Behandlingsgrunnlag> findByBehandling_Id(long behandlingId);
}
