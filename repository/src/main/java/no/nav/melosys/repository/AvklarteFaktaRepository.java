package no.nav.melosys.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvklarteFaktaRepository extends JpaRepository<Avklartefakta, Long> {

    Set<Avklartefakta> findByBehandlingsresultatId(long behandlingsid);

    Optional<Avklartefakta> findByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatype avklartefaktaType);

    Set<Avklartefakta> findAllByBehandlingsresultatIdAndType(long behandlingsid, Avklartefaktatype avklartefaktaType);

    Set<Avklartefakta> findAllByBehandlingsresultatIdAndTypeIn(long behandlingsid, Collection<Avklartefaktatype> avklartefaktatyper);

    Set<Avklartefakta> findByBehandlingsresultatIdAndTypeAndFakta(long behandlingsid,
                                                                  Avklartefaktatype type,
                                                                  String fakta);

    void deleteByBehandlingsresultatId(long behandlingsid);
}
