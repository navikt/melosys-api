package no.nav.melosys.integrasjon.aareg

import no.nav.melosys.domain.Saksopplysning
import java.time.LocalDate

/**
 * Fasade mot Arbeidsforholdsregisteret (AAREG)
 */
interface AaregFasade {
    /**
     * Etterspør en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     *
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     */
    fun finnArbeidsforholdPrArbeidstaker(ident: String, fom: LocalDate?, tom: LocalDate?): Saksopplysning
}
