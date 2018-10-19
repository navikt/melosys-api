package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface MedlFasade {

    Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException;

    Long opprettPeriode(String fnr, Medlemsperiode medlemsperiode) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException;
}
