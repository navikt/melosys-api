package no.nav.melosys.integrasjon.aareg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;

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
    Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, String regelverk, LocalDate fom, LocalDate tom) throws IntegrasjonException, TekniskException, SikkerhetsbegrensningException;

    /**
     * Etterspør et arbeidsforhold fra AA-registeret med gjeldende og historiske arbeidsavtaler.
     *
     * @param arbeidsforholdsID Unik ID til et arbeidsforhold i NAV
     */
    Saksopplysning hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws IntegrasjonException, SikkerhetsbegrensningException;
}
