package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.adresse.Adresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.adresse.PersonAdresse
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * Adressekandidater til registeret. Kandidatlistene finnes bare på PDL-representasjonen
 * (Personopplysninger); andre persondatakilder har «gjeldende adresse» som eneste alternativ.
 * erGyldig() er samme filter som resten av Melosys bruker – det luker ut både historiske og utgåtte adresser.
 * Sorteringen er defensiv – registrertDato kan mangle, og hentRegistrertDato() ville kastet.
 */
internal fun Persondata.oppholdsadresseKandidater(): List<Oppholdsadresse> =
    ((this as? Personopplysninger)
        ?.oppholdsadresser
        ?.sortedWith(compareByDescending(nullsFirst<LocalDateTime>()) { it.registrertDato })
        ?: listOfNotNull(finnOppholdsadresse().getOrNull()))
        .filter { it.erGyldig() }

internal fun Persondata.kontaktadresseKandidater(): List<Kontaktadresse> =
    ((this as? Personopplysninger)
        ?.kontaktadresser
        ?.sortedWith(compareByDescending(nullsFirst<LocalDateTime>()) { it.registrertDato })
        ?: listOfNotNull(finnKontaktadresse().getOrNull()))
        .filter { it.erGyldig() }

/**
 * Adressen som én linje. Feltene og rekkefølgen er de samme som i StrukturertAdresse.toList(), med c/o først
 * slik Postadresse.lagPostadresse legger det, men med dekodet landnavn: Land_iso2.valueOf() kaster for landkoder
 * som ikke finnes i enumen.
 */
internal fun PersonAdresse.tilAdresselinje(landnavn: (String?) -> String): String? =
    adresselinjensAdresse()?.let { adresse ->
        listOf(
            coAdressenavn,
            adresse.tilleggsnavn,
            Adresse.sammenslå(adresse.gatenavn, adresse.husnummerEtasjeLeilighet),
            adresse.postboks,
            adresse.postnummer,
            adresse.poststed,
            adresse.region,
            landnavn(adresse.landkode),
        ).filterNot { it.isNullOrBlank() }.joinToString(", ")
    }

/** Kontaktadressen kan komme semistrukturert – samme utledning som Personopplysninger.lagPostadresseFraKontaktadresse. */
private fun PersonAdresse.adresselinjensAdresse(): StrukturertAdresse? =
    (this as? Kontaktadresse)?.hentEllerLagStrukturertAdresse() ?: strukturertAdresse
