package no.nav.melosys.repository;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaType;
import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface AvklarteFaktaRepository extends CrudRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultat(Behandlingsresultat resultat);

    Optional<Avklartefakta> findByBehandlingsresultatAndType(Behandlingsresultat resultat, AvklartefaktaType avklartefaktaType);

    Optional<Avklartefakta> findByBehandlingsresultatAndReferanse(Behandlingsresultat resultat, String referanse);

    @Query("select a from Avklartefakta a where a.behandlingsresultat = :behandlingsid and a.type = :fakta_type")
    Optional<Avklartefakta> findByBehandlingsidAndType(@Param("behandlingsid") long behandlingsid, @Param("fakta_type") AvklartefaktaType fakta_type);
}
