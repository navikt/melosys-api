package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface MedlFasade {

    Saksopplysning getPeriodeListe(String fnr) throws IntegrasjonException, SikkerhetsbegrensningException;
}
