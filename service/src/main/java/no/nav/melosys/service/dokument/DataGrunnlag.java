package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.behandling.Behandling;

public interface DataGrunnlag {
    Behandling getBehandling();
    BostedGrunnlag getBostedGrunnlag();
}
