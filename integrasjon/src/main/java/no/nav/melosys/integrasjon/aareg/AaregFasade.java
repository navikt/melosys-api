package no.nav.melosys.integrasjon.aareg;

import java.util.List;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;

/**
 * Fasade mot Arbeidsforholdsregisteret (AAREG)
 */
public interface AaregFasade {

    /**
     * Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen)
     */
    public static final String REGELVERK_A_ORDNINGEN = "A_ORDNINGEN";

    /**
     * Etterspør og returnerer en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     * @param regelverk Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen). Mulige verdier: FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
     *
     * @return
     */
    @Deprecated
    List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String ident, String regelverk) throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput;

    /**
     * Etterspør en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     * @param regelverk Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen). Mulige verdier: FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
     *
     * @return Returnerer en {@code Saksopplysning}
     */
    Saksopplysning getArbeidsforholdPrArbeidstaker(String ident, String regelverk) throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput;
}
