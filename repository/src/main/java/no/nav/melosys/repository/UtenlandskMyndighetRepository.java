package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.springframework.data.repository.CrudRepository;

public interface UtenlandskMyndighetRepository extends CrudRepository<UtenlandskMyndighet, Long> {

    UtenlandskMyndighet findByLandkode(Landkoder landkode);

    List<UtenlandskMyndighet> findAll();
}
