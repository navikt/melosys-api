package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;

public interface SedRuter {
    void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) throws MelosysException;
}
