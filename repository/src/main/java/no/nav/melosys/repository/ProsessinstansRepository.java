package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProsessinstansRepository extends JpaRepository<Prosessinstans, Long> {
    @Query("SELECT NEW no.nav.melosys.repository.ProsessinstansAntall(p.type, p.steg, COUNT(p)) FROM Prosessinstans p "
        + "WHERE p.steg <> no.nav.melosys.domain.ProsessSteg.FERDIG GROUP BY p.type, p.steg")
    List<ProsessinstansAntall> antallAktiveOgFeiletPerTypeOgSteg();
    Optional<Prosessinstans> findByBehandling_IdAndStegIsNotAndStegIsNot(long id, ProsessSteg prosessSteg1, ProsessSteg prosessSteg2);
    Optional<Prosessinstans> findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(ProsessType prosessType, long id, ProsessSteg prosessSteg1, ProsessSteg prosessSteg2);
    List<Prosessinstans> findAllByStegIsNotAndStegIsNot(ProsessSteg prosessSteg1, ProsessSteg prosessSteg2);
}
