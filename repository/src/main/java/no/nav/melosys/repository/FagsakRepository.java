package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FagsakRepository extends CrudRepository<Fagsak, Long> {

    Optional<Fagsak> findBySaksnummer(String saksnummer);

    Optional<Fagsak> findByGsakSaksnummer(Long gsakSaksnummer);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.aktørId = :id") //$NON-NLS-1$
    List<Fagsak> findByRolleAndAktør(@Param("rolle") Aktoersroller rolle, @Param("id") String aktørID);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.orgnr = :id") //$NON-NLS-1$
    List<Fagsak> findByRolleAndOrgnr(@Param("rolle") Aktoersroller rolle, @Param("id") String orgnr);

    @Query(value = "SELECT saksnummer_seq.nextval FROM dual", nativeQuery = true)
    Long hentNesteSekvensVerdi();
}
