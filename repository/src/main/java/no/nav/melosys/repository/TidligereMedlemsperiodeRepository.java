package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.TidligereMedlemsperiode;
import no.nav.melosys.domain.TidligereMedlemsperiodeId;
import org.springframework.data.repository.CrudRepository;

public interface TidligereMedlemsperiodeRepository extends CrudRepository<TidligereMedlemsperiode, TidligereMedlemsperiodeId> {

    void deleteById_BehandlingId(long behandlingId);

    List<TidligereMedlemsperiode> findById_BehandlingId(long behandlingId);
}
