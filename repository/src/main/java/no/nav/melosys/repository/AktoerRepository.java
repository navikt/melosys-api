package no.nav.melosys.repository;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AktoerRepository extends JpaRepository<Aktoer, Long> {
    List<Aktoer> findByFagsakAndFullmakterIsNotEmpty(Fagsak fagsak);

    List<Aktoer> findByFagsak(Fagsak fagsak);

    List<Aktoer> findByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteAllByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteByAktørId(String aktørId);

    /**
     * Overstyrer registrertDato direkte i basen. Brukes når en aktør skal få registreringsdato
     * lik mottaksdato fra søknad i stedet for INSERT-tidspunktet auditing setter.
     * Bypasser @Column(updatable = false) og Envers — endringen logges ikke i aktoer_aud.
     */
    @Modifying
    @Query(value = "UPDATE aktoer SET registrert_dato = :dato WHERE id = :id", nativeQuery = true)
    void overstyrRegistrertDato(@Param("id") Long id, @Param("dato") Instant dato);
}
