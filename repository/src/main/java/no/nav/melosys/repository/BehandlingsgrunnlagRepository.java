package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehandlingsgrunnlagRepository extends JpaRepository<Behandlingsgrunnlag, Long> {

    void deleteByBehandling_Id(long behandlingId);

    Optional<Behandlingsgrunnlag> findByBehandling_Id(long behandlingId);

    List<Behandlingsgrunnlag> findByEksternReferanseID(String eksternReferanseID);
}
