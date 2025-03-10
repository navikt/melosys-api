package no.nav.melosys.saksflytapi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProsessinstansForServiceRepository extends JpaRepository<Prosessinstans, UUID> {
    Optional<Prosessinstans> findByBehandling_IdAndTypeIn(long id, ProsessType... prosessTypes);

    @Query(value = "SELECT * FROM PROSESSINSTANS " +
        "WHERE PROSESS_TYPE IN ('SATSENDRING', 'SATSENDRING_TILBAKESTILL_NY_VURDERING') " +
        "AND STATUS != 'FERDIG' " +
        "AND DBMS_LOB.INSTR(DATA, 'opprinneligBeh=' || :behandlingID) > 0",
        nativeQuery = true)
    List<Prosessinstans> findBySatsendringAndOpprinneligBehandlingIdNotFerdig(@Param("behandlingID") Long behandlingID);
}
