package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;

public interface MedlFasade {

    Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException;

    Long opprettPeriodeEndelig(String fnr, Lovvalgsperiode lovvalgsperiode) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException;

    Long opprettPeriodeUnderAvklaring(String fnr, Lovvalgsperiode lovvalgsperiode) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException;
}
