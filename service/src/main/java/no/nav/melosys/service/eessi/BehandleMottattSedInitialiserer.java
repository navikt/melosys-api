package no.nav.melosys.service.eessi;


import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.exception.TekniskException;

public interface BehandleMottattSedInitialiserer {

    void initialiserProsessinstans(Prosessinstans prosessinstans) throws TekniskException;

    boolean gjelderSedType(SedType sedType);
}
