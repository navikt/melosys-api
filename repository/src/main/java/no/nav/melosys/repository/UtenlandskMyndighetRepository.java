package no.nav.melosys.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import org.springframework.data.repository.CrudRepository;

public interface UtenlandskMyndighetRepository extends CrudRepository<UtenlandskMyndighet, Long> {

    Optional<UtenlandskMyndighet> findByLandkode(Land_iso2 landkode);

    List<UtenlandskMyndighet> findAll();

    List<UtenlandskMyndighet> findByLandkodeIsIn(Collection<Land_iso2> landkoder);
}
