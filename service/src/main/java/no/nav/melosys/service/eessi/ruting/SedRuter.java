package no.nav.melosys.service.eessi.ruting;


import no.nav.melosys.saksflytapi.domain.Prosessinstans;

public interface SedRuter {
    void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID);
}
