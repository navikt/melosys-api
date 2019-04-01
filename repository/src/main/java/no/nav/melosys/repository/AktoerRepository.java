package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface AktoerRepository extends JpaRepository<Aktoer, Long> {

    Optional<Aktoer> findByFagsakAndRolleAndRepresenterer(Fagsak fagsak, Aktoersroller aktoersroller, Representerer representerer);

    @Modifying
    @Query("delete from Aktoer a where a.fagsak = :fagsak and a.rolle = :rolle")
    void deleteAllByFagsakAndRolle(@Param("fagsak")Fagsak fagsak, @Param("rolle") Aktoersroller aktoersroller);

    @Modifying
    @Query("delete from Aktoer a where a.id = ?#{#aktoerParam.id}")
    @Transactional(propagation = Propagation.REQUIRED)
    void deleteById(@Param("aktoerParam") Aktoer aktoer);
}
