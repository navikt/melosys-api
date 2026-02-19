package no.nav.melosys.domain.mottatteopplysninger.utsendtarbeidstaker

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData

/**
 * Saksopplysninger fra digital A1-søknad for utsendte arbeidstakere (EØS).
 *
 * Erstatter [no.nav.melosys.domain.mottatteopplysninger.Soeknad] for den digitale søknadsflyten
 * (MELOSYS_MOTTAK_DIGITAL_SØKNAD). [Soeknad][no.nav.melosys.domain.mottatteopplysninger.Soeknad]
 * ble opprinnelig laget for Altinn-søknader og inneholder felter som
 * [no.nav.melosys.domain.mottatteopplysninger.data.ArbeidsgiversBekreftelse] som ikke er
 * relevante for digitale søknader. Denne klassen gir en renere, immutabel modell tilpasset
 * den nye flyten, uten Altinn-spesifikke felter.
 *
 * De tre egne feltene er `val` (immutabel) og må settes via constructor. Arvede parent-felter
 * fra [MottatteOpplysningerData] er mutable (`public var` i Java) og settes via `apply { ... }`
 * i mapperen.
 *
 * JSON-kompatibel med [Soeknad][no.nav.melosys.domain.mottatteopplysninger.Soeknad] —
 * eksisterende data serialisert fra Soeknad kan deserialiseres til denne klassen (og omvendt)
 * takket være [com.fasterxml.jackson.annotation.JsonIgnoreProperties] på [MottatteOpplysningerData].
 *
 * @see no.nav.melosys.domain.mottatteopplysninger.Soeknad den gamle Altinn-varianten
 * @see no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerKonverterer for type-til-klasse-mapping ved deserialisering
 */
class UtsendtArbeidstakerSøknad(
    val loennOgGodtgjoerelse: LoennOgGodtgjoerelseUtsendtArbeidstaker? = LoennOgGodtgjoerelseUtsendtArbeidstaker(),
    val utenlandsoppdraget: UtenlandsoppdragetUtsendtArbeidstaker = UtenlandsoppdragetUtsendtArbeidstaker(),
    val arbeidssituasjonOgOevrig: ArbeidssituasjonOgOevrigUtsendtArbeidstaker = ArbeidssituasjonOgOevrigUtsendtArbeidstaker()
) : MottatteOpplysningerData()
