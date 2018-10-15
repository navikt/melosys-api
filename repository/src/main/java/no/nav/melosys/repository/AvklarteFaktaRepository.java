package no.nav.melosys.repository;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaType;
import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.repository.CrudRepository;

public interface AvklarteFaktaRepository extends CrudRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultatId(long behandlingsid);

    Optional<Avklartefakta> findByBehandlingsresultatIdAndType(long behandlingsid, AvklartefaktaType avklartefaktaType);

    Optional<Avklartefakta> findByBehandlingsresultatAndReferanseAndSubjekt(Behandlingsresultat resultat,
                                                                            String referanse,
                                                                            String subjekt);
}
