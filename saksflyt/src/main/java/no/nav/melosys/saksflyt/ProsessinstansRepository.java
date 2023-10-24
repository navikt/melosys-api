package no.nav.melosys.saksflyt;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.domain.metrikker.ProsessinstansStegAntall;
import no.nav.melosys.domain.saksflyt.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProsessinstansRepository extends JpaRepository<Prosessinstans, UUID> {
    @Query("SELECT NEW no.nav.melosys.domain.metrikker.ProsessinstansAntall(p.type, p.status, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.type IN (?1) "
        + "AND p.status <> no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG GROUP BY p.type, p.status")
    Collection<ProsessinstansAntall> antallAktiveOgFeiletPerTypeOgStatus(Collection<ProsessType> typer);

    @Query("SELECT NEW no.nav.melosys.domain.metrikker.ProsessinstansStegAntall(p.sistFullførtSteg, p.type, p.status, COUNT(p)) "
        + "FROM Prosessinstans p "
        + "WHERE p.status <> no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG "
        + "AND p.sistFullførtSteg IN (?1) OR ((?2) = TRUE AND p.sistFullførtSteg IS NULL) "
        + "GROUP BY p.type, p.status, p.sistFullførtSteg")
    Collection<ProsessinstansStegAntall> antallAktiveOgFeiletPerStegOgStatus(Collection<ProsessSteg> prosessSteg,
                                                                             boolean taMedForsteStegIProsessFlyt);

    Optional<Prosessinstans> findByBehandling_IdAndStatusIs(long id, ProsessStatus prosessStatus);

    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);

    Collection<Prosessinstans> findAllByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);

    Collection<Prosessinstans> findAllByStatus(ProsessStatus status);

    Collection<Prosessinstans> findAllByStatusIn(Set<ProsessStatus> statuses);

    @Query("""
        SELECT NEW no.nav.melosys.domain.saksflyt.ProsessinstansInfo(p.id, p.status, p.registrertDato, p.låsReferanse) FROM Prosessinstans p
        WHERE p.id <> ?1 AND p.status NOT IN (?2) AND p.låsReferanse LIKE CONCAT(?3, '%')
        """)
    Collection<ProsessinstansInfo> findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(UUID id, Collection<ProsessStatus> prosessStatus, String låsReferanse);

    Collection<Prosessinstans> findAllByStatusNotInAndLåsReferanseStartingWith(Collection<ProsessStatus> prosessStatus, String låsReferanse);

    Collection<Prosessinstans> findAllByLåsReferanseStartingWith(String låsReferanse);

    Collection<Prosessinstans> findAllByBehandling_Id(long id);

    @Query(value = "SELECT * FROM PROSESSINSTANS p WHERE p.PROSESS_TYPE = 'MOTTAK_SED' AND p.\"DATA\" LIKE '%X100%'",
        nativeQuery = true)
    Set<Prosessinstans> findAllWithSedX100();

    boolean existsByStatusNotInAndLåsReferanse(Collection<ProsessStatus> prosessStatus, String låsreferanse);

    @Query(value = "SELECT * FROM PROSESSINSTANS p WHERE p.REGISTRERT_DATO > ?1", nativeQuery = true)
    Collection<Prosessinstans> findAllAfterDate(LocalDateTime localDateTime);
}
