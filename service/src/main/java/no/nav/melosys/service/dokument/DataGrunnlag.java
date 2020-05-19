package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;

public interface DataGrunnlag {
    Behandling getBehandling();
    BostedGrunnlag getBostedGrunnlag();
}
