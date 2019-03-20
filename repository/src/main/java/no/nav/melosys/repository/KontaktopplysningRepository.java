package no.nav.melosys.repository;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.KontaktopplysningID;
import org.springframework.data.repository.CrudRepository;

public interface KontaktopplysningRepository extends CrudRepository<Kontaktopplysning, KontaktopplysningID> {
}
