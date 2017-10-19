package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

public interface Medl2Fasade {

    Saksopplysning getPeriodeListe(String fnr) throws IntegrasjonException, SikkerhetsbegrensningException;
}
