package no.nav.melosys.repository;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProsessinstansRepository extends JpaRepository<Prosessinstans, Long> {
    @Query("SELECT NEW no.nav.melosys.repository.ProsessinstansAntall(p.type, p.status, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.status = no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG GROUP BY p.type, p.status")
    Collection<ProsessinstansAntall> antallAktiveOgFeiletPerTypeOgStatus();
    Optional<Prosessinstans> findByBehandling_IdAndStegIsNotAndStegIsNot(long id, ProsessSteg prosessSteg1, ProsessSteg prosessSteg2);
    Optional<Prosessinstans> findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(ProsessType prosessType, long id, ProsessSteg prosessSteg1, ProsessSteg prosessSteg2);
    Collection<Prosessinstans> findAllByStatus(ProsessStatus status);
}
