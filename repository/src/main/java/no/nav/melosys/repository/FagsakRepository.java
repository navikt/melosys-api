package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;

public interface FagsakRepository extends CrudRepository<Fagsak, Long> {

    Fagsak findBySaksnummer(Long saksnummer);

    @Query("select f from Fagsak f, Aktoer a where a.fagsak = f and a.rolle = :rolle  and a.aktørId = :id") //$NON-NLS-1$
    List<Fagsak> findByRolleAndAktør(@Param("rolle") RolleType rolle, @Param("id") String aktørID);
}
