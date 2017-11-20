package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

public interface MedlFasade {

    Saksopplysning getPeriodeListe(String fnr) throws IntegrasjonException, SikkerhetsbegrensningException;
}
