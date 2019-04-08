package no.nav.melosys.integrasjon.aareg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;

import java.time.LocalDate;

/**
 * Fasade mot Arbeidsforholdsregisteret (AAREG)
 */
public interface AaregFasade {

    /**
     * Etterspør en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     */
    Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, LocalDate fom, LocalDate tom) throws TekniskException, SikkerhetsbegrensningException;

    /**
     * Etterspør et arbeidsforhold fra AA-registeret med gjeldende og historiske arbeidsavtaler.
     *
     * @param arbeidsforholdsID Unik ID til et arbeidsforhold i NAV
     */
    Saksopplysning hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException;
}
