package no.nav.melosys.repository.tekstblokk;

import java.util.List;

import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TekstblokkRepository extends JpaRepository<Tekstblokk, Long> {

    List<Tekstblokk> findAllByOrderByTittelAsc();

    List<Tekstblokk> findAllByTypeOrderByTittelAsc(TekstblokkType type);
}
