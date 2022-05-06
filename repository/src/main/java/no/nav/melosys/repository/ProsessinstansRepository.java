package no.nav.melosys.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.domain.metrikker.ProsessinstansStegAntall;
import no.nav.melosys.domain.saksflyt.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProsessinstansRepository extends JpaRepository<Prosessinstans, UUID> {
    @Query("SELECT NEW no.nav.melosys.domain.metrikker.ProsessinstansAntall(p.type, p.status, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.status <> no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG "
        + "AND p.type IN (?1) GROUP BY p.type, p.status")
    Collection<ProsessinstansAntall> antallAktiveOgFeiletPerTypeOgStatus(Collection<ProsessType> typer);

    @Query("SELECT NEW no.nav.melosys.domain.metrikker.ProsessinstansStegAntall(p.sistFullførtSteg, p.type, p.status, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.status <> no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG "
        + "AND p.sistFullførtSteg IN (?1) GROUP BY p.type, p.sistFullførtSteg, p.status")
    Collection<ProsessinstansStegAntall> antallAktiveOgFeiletPerStegOgStatus(Collection<ProsessSteg> prosessSteg);

    Optional<Prosessinstans> findByBehandling_IdAndStatusIs(long id, ProsessStatus prosessStatus);

    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);

    Collection<Prosessinstans> findAllByStatus(ProsessStatus status);

    @Query("""
        SELECT NEW no.nav.melosys.domain.saksflyt.ProsessinstansInfo(p.id, p.status, p.registrertDato, p.låsReferanse) FROM Prosessinstans p
        WHERE p.id <> ?1 AND p.status NOT IN (?2) AND p.låsReferanse LIKE CONCAT(?3, '%')
        """)
    Collection<ProsessinstansInfo> findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(UUID id, Collection<ProsessStatus> prosessStatus, String låsReferanse);

    Collection<Prosessinstans> findAllByStatusNotInAndLåsReferanseStartingWith(Collection<ProsessStatus> prosessStatus, String låsReferanse);

    Collection<Prosessinstans> findAllByLåsReferanseStartingWith(String låsReferanse);

    boolean existsByStatusNotInAndLåsReferanse(Collection<ProsessStatus> prosessStatus, String låsreferanse);
}
