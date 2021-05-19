package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;

/**
 * Fasade mot Arbeidsforholdsregisteret (AAREG)
 */
public interface AaregFasade {

    /**
     * Etterspør en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     */
    Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, LocalDate fom, LocalDate tom);

    /**
     * Etterspør et arbeidsforhold fra AA-registeret med gjeldende og historiske arbeidsavtaler.
     *
     * @param arbeidsforholdsID Unik ID til et arbeidsforhold i NAV
     */
    Saksopplysning hentArbeidsforholdHistorikk(Long arbeidsforholdsID);
}
