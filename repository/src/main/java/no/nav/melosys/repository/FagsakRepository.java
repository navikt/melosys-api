package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Fagsak;

public interface FagsakRepository extends CrudRepository<Fagsak, Long> {

    Fagsak findBySaksnummer(Long saksnummer);

    @Query("select f from Fagsak f, Bruker b where f.bruker = b and b.fnr = ?1") //$NON-NLS-1$
    List<Fagsak> findByFnr(String fnr);
}
