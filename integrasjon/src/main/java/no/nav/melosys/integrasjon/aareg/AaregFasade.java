package no.nav.melosys.integrasjon.aareg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

import java.time.LocalDate;

/**
 * Fasade mot Arbeidsforholdsregisteret (AAREG)
 */
public interface AaregFasade {

    /**
     * Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen)
     */
    public static final String REGELVERK_A_ORDNINGEN = "A_ORDNINGEN";

    /**
     * Etterspør en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     * @param regelverk Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen). Mulige verdier: FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
     */
    Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, String regelverk) throws IntegrasjonException, SikkerhetsbegrensningException;

    Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, String regelverk, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException;
}
