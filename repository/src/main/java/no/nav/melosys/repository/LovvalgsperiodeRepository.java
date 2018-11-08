package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;

public interface LovvalgsperiodeRepository extends CrudRepository<Lovvalgsperiode, Long> {

    @Transactional(propagation = Propagation.SUPPORTS)
    List<Lovvalgsperiode> findByBehandlingsresultatId(long id);

    // Må her bruke ett skreddersydd query p.g.a. en bug i Spring Data og/eller
    // JPA/Hibernate. Den automatisk genererte metoden (uten @Query) blir ikke
    // flushet/committet i tide før en påfølgende lagreoperasjon (e.g.
    // CrudRepository.save()). Er muligens relatert til:
    // https://jira.spring.io/browse/DATAJPA-727
    @Modifying
    @Query("delete from Lovvalgsperiode l where l.behandlingsresultat.id = ?#{#bhndlngsRes.id}")
    @Transactional(propagation = Propagation.REQUIRED)
    int deleteByBehandlingsresultat(@Param("bhndlngsRes") Behandlingsresultat behandlingsresultat);
}
