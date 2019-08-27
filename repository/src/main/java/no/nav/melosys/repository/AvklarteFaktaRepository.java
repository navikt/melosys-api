package no.nav.melosys.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvklarteFaktaRepository extends JpaRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultatId(long behandlingsid);

    Optional<Avklartefakta> findByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatyper avklartefaktaType);

    Set<Avklartefakta> findAllByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatyper avklartefaktaType);

    Set<Avklartefakta> findAllByBehandlingsresultatIdAndTypeIn(long behandlingsid, Collection<Avklartefaktatyper> avklartefaktatyper);

    Set<Avklartefakta> findByBehandlingsresultatIdAndTypeAndFakta(long behandlingsid,
                                                                  Avklartefaktatyper type,
                                                                  String fakta);

    void deleteByBehandlingsresultatId(long behandlingsid);
}
