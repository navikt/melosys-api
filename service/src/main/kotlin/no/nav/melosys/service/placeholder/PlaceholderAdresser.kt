package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.adresse.Adresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * Adressekandidater til registeret. Kandidatlistene finnes bare på PDL-representasjonen
 * (Personopplysninger); andre persondatakilder har «gjeldende adresse» som eneste alternativ.
 * Sorteringen er defensiv – registrertDato kan mangle, og hentRegistrertDato() ville kastet.
 */
internal fun Persondata.oppholdsadresseKandidater(): List<Oppholdsadresse> =
    (this as? Personopplysninger)
        ?.oppholdsadresser
        ?.filterNot { it.erHistorisk }
        ?.sortedWith(compareByDescending(nullsFirst<LocalDateTime>()) { it.registrertDato })
        ?: listOfNotNull(finnOppholdsadresse().getOrNull())

internal fun Persondata.kontaktadresseKandidater(): List<Kontaktadresse> =
    (this as? Personopplysninger)
        ?.kontaktadresser
        ?.filterNot { it.erHistorisk }
        ?.sortedWith(compareByDescending(nullsFirst<LocalDateTime>()) { it.registrertDato })
        ?: listOfNotNull(finnKontaktadresse().getOrNull())

/**
 * Adressen som én linje etter mønsteret i StrukturertAdresse.toString(), men med dekodet landnavn:
 * Land_iso2.valueOf() kaster for landkoder som ikke finnes i enumen.
 */
internal fun StrukturertAdresse.tilAdresselinje(landnavn: (String?) -> String): String =
    listOf(
        tilleggsnavn,
        Adresse.sammenslå(gatenavn, husnummerEtasjeLeilighet),
        postboks,
        postnummer,
        poststed,
        region,
        landnavn(landkode),
    ).filterNot { it.isNullOrBlank() }.joinToString(", ")
