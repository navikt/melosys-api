package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.Land_ISO2;
import org.springframework.data.repository.CrudRepository;

public interface UtenlandskMyndighetRepository extends CrudRepository<UtenlandskMyndighet, Long> {

    Optional<UtenlandskMyndighet> findByLandkode(Land_ISO2 landkode);

    List<UtenlandskMyndighet> findAll();

    List<UtenlandskMyndighet> findByLandkodeIsIn(Collection<Land_ISO2> landkoder);
}
