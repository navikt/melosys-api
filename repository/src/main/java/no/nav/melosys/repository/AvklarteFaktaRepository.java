package no.nav.melosys.repository;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface AvklarteFaktaRepository extends CrudRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultatId(long behandlingsid);

    Optional<Avklartefakta> findByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatype avklartefaktaType);

    Set<Avklartefakta> findAllByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatype avklartefaktaType);

    Set<Avklartefakta> findByBehandlingsresultatIdAndTypeAndFakta(long behandlingsid,
                                                                  Avklartefaktatype type,
                                                                  String fakta);

    // Må her bruke ett skreddersydd query p.g.a. en bug i Spring Data og/eller
    // JPA/Hibernate. Den automatisk genererte metoden (uten @Query) blir ikke
    // flushet/committet i tide før en påfølgende lagreoperasjon (e.g.
    // CrudRepository.save()). Er muligens relatert til:
    // https://jira.spring.io/browse/DATAJPA-727
    @Modifying
    @Query("delete from Avklartefakta l where l.behandlingsresultat.id = ?#{#bhndlngsRes.id}")
    @Transactional(propagation = Propagation.REQUIRED)
    void deleteByBehandlingsresultat(@Param("bhndlngsRes") Behandlingsresultat behandlingsresultat);
}
