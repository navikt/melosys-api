package no.nav.melosys.repository;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AvklarteFaktaRepository extends CrudRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultatId(long behandlingsid);

    Optional<Avklartefakta> findByBehandlingsresultatIdAndType(long behandlingsid, AvklartefaktaType avklartefaktaType);

    Optional<Avklartefakta> findByBehandlingsresultatAndReferanseAndSubjekt(Behandlingsresultat resultat,
                                                                            String referanse,
                                                                            String subjekt);

    @Query("select af from Avklartefakta where af.type = 'AVKLARTE_ARBEIDSGIVER' and af.fakta = 'TRUE'")
    Set<Avklartefakta> hentAvklarteOrganisasjoner(long behandlingsid);
}
