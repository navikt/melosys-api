package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FagsakRepository extends CrudRepository<Fagsak, Long> {

    Fagsak findBySaksnummer(String saksnummer);

    Fagsak findByGsakSaksnummer(Long gsakSakID);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.aktørId = :id") //$NON-NLS-1$
    List<Fagsak> findByRolleAndAktør(@Param("rolle") RolleType rolle, @Param("id") String aktørID);

    @Query(value = "SELECT saksnummer_seq.nextval FROM dual", nativeQuery = true)
    Long hentNesteSekvensVerdi();


    @Query("select f from Fagsak f, Behandling b where b.fagsak = f and b.id = :id")
    List<Fagsak> findByBehandlingsId(@Param("id") long behandlingsId);
}

