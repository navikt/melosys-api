package no.nav.melosys.service.persondata

import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Personstatuser
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.familie.Familiemedlem
import no.nav.melosys.domain.person.familie.Familierelasjon
import java.time.LocalDate
import java.time.LocalDateTime

object PersonopplysningerObjectFactory {
    const val ADRESSELINJE_1_BRUKER = "Andebygata 1"
    const val POSTNR_BRUKER = "9999"
    const val POSTSTED_BRUKER = "Andeby"
    const val REGION = "NEVERLAND"
    const val HUSNUMMER = "3"

    fun lagPersonopplysninger(): Personopplysninger =
        lagPersonopplysninger(false, false, false, false, false)

    fun lagPersonopplysningerStatløs(): Personopplysninger =
        lagPersonopplysninger(true, false, false, false, false)

    fun lagPersonopplysningerUtenBostedsadresse(): Personopplysninger =
        lagPersonopplysninger(false, true, false, false, false)

    fun lagPersonopplysningerUtenOppholdsadresse(): Personopplysninger =
        lagPersonopplysninger(false, false, true, false, false)

    fun lagPersonopplysningerUtenKontaktadresse(): Personopplysninger =
        lagPersonopplysninger(false, false, false, true, false)

    fun lagPersonopplysningerUtenBostedsadresseOgKontaktadresse(): Personopplysninger =
        lagPersonopplysninger(false, true, false, true, false)

    fun lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse(): Personopplysninger =
        lagPersonopplysninger(false, false, true, true, false)

    fun lagPersonopplysningerMedFamilie(): Personopplysninger =
        lagPersonopplysninger(false, false, false, false, true)

    fun lagPersonopplysningerKontaktadresseSemistrukturert(erUtenOppholdsadresse: Boolean): Personopplysninger =
        Personopplysninger(
            emptyList(), lagBostedsadresse(), null, emptySet(),
            lagFødsel(), null, lagKjønn(), lagKontaktadresser(true), lagNavn(),
            if (erUtenOppholdsadresse) emptySet() else lagOppholdsadresser(),
            lagStatsborgerskap(false)
        )

    private fun lagPersonopplysninger(
        erStatløs: Boolean, erUtenBostedsadresse: Boolean, erUtenOppholdsadresse: Boolean,
        erUtenKontaktadresse: Boolean, harFamilie: Boolean
    ) = Personopplysninger(
        emptyList(),
        if (erUtenBostedsadresse) null else lagBostedsadresse(),
        null,
        if (harFamilie) setOf(lagRelatertVedsivilstand(), lagBarn()) else emptySet(),
        lagFødsel(),
        null,
        lagKjønn(),
        if (erUtenKontaktadresse) emptySet() else lagKontaktadresser(false),
        lagNavn(),
        if (erUtenOppholdsadresse) emptySet() else lagOppholdsadresser(),
        lagStatsborgerskap(erStatløs)
    )

    fun lagPersonopplysningerUtenAdresser() = Personopplysninger(
        emptyList(), null, null, emptySet(), lagFødsel(), null,
        lagKjønn(), emptyList(), lagNavn(), emptyList(), lagStatsborgerskap(false)
    )

    private fun lagFødsel() = Foedsel(LocalDate.EPOCH, null, null, null)

    private fun lagKjønn(): KjoennType = KjoennType.UKJENT

    private fun lagBostedsadresse(): Bostedsadresse = Bostedsadresse(
        StrukturertAdresse("gatenavnFraBostedsadresse", "3", "1234", "Oslo", "Norge", "NO"),
        null, null, null, null, null, false
    )

    fun lagKontaktadresser(semistruktuert: Boolean): Collection<Kontaktadresse> =
        if (semistruktuert) {
            setOf(
                Kontaktadresse(
                    null,
                    lagSemistrukturertAdresse(),
                    null,
                    null,
                    null,
                    Master.PDL.name,
                    null,
                    LocalDateTime.MAX.minusYears(43),
                    false
                )
            )
        } else {
            setOf(
                Kontaktadresse(
                    lagStrukturertAdresse("gatenavnKontaktadressePDL"),
                    null,
                    null,
                    null,
                    null,
                    Master.PDL.name,
                    null,
                    LocalDateTime.MAX.minusYears(42),
                    false
                ),
                Kontaktadresse(
                    lagStrukturertAdresse("gammelGatenavnKontaktadressePDL"),
                    null,
                    null,
                    null,
                    null,
                    Master.PDL.name,
                    null,
                    LocalDateTime.MIN,
                    false
                ),
                Kontaktadresse(
                    lagStrukturertAdresse("gatenavnKontaktadresseFreg"),
                    null,
                    null,
                    null,
                    null,
                    Master.FREG.name,
                    null,
                    LocalDateTime.MAX,
                    false
                )
            )
        }

    private fun lagOppholdsadresser(): Collection<Oppholdsadresse> =
        setOf(
            Oppholdsadresse(
                lagStrukturertAdresse("gammelGatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MIN,
                false
            ),
            Oppholdsadresse(
                lagStrukturertAdresse("gatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MAX,
                false
            )
        )

    private fun lagStrukturertAdresse(gatenavn: String): StrukturertAdresse =
        StrukturertAdresse(gatenavn, null, "0123", "Poststed", null, "NO")

    private fun lagSemistrukturertAdresse(): SemistrukturertAdresse =
        SemistrukturertAdresse("Kranstien 3", "0338 Oslo", null, null, "0338", null, Landkoder.NO.name)

    private fun lagNavn(): Navn = Navn("Ola", null, "Nordmann")

    private fun lagStatsborgerskap(erStatløs: Boolean): Collection<Statsborgerskap> =
        if (erStatløs) {
            listOf(
                Statsborgerskap(
                    Land.STATSLØS,
                    null,
                    LocalDate.EPOCH,
                    LocalDate.now(),
                    "PDL",
                    "Dolly",
                    false
                )
            )
        } else {
            listOf(
                Statsborgerskap(
                    "NOR", null, LocalDate.parse("2009-11-18"),
                    LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
                ),
                Statsborgerskap(
                    "SWE", null, LocalDate.parse("1979-11-18"),
                    LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
                ),
                Statsborgerskap(
                    "DNK", null, null,
                    LocalDate.parse("1980-11-18"), "PDL",
                    "Dolly", false
                )
            )
        }

    fun lagPersonMedHistorikk(): PersonMedHistorikk {
        val bostedsadresse1 = Bostedsadresse(
            StrukturertAdresse("gate1", "42 C", null, null, null, null),
            null, null, null, "PDL", null, false
        )
        val bostedsadresse2 = Bostedsadresse(
            StrukturertAdresse("gate2", null, null, null, null, null),
            null, null, null, null, null, true
        )

        val kontaktadresse1 = Kontaktadresse(
            StrukturertAdresse("kontakt 1", null, null, null, null, null), null, null, null, null, "PDL", null, null,
            false
        )
        val kontaktadresse2 = Kontaktadresse(
            null,
            SemistrukturertAdresse("kontakt 2", "linje 2", null, null, "1234", "By", "IT"), null, null, null, null,
            null, null, false
        )

        val oppholdsadresse1 = Oppholdsadresse(
            StrukturertAdresse("opphold 1", null, null, null, null, null), null, null, null, "PDL", null, null,
            false
        )
        val oppholdsadresse2 = Oppholdsadresse(
            StrukturertAdresse("opphold 2", null, null, null, null, null), null, null, null, null, null, null,
            true
        )

        val statsborgerskap1 = Statsborgerskap(
            "AAA", null, LocalDate.parse("2009-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
        )
        val statsborgerskap2 = Statsborgerskap(
            "BBB", null, LocalDate.parse("1979-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
        )
        val statsborgerskap3 = Statsborgerskap(
            "CCC", null, null, LocalDate.parse("1980-11-18"), "PDL",
            "Dolly", false
        )

        return PersonMedHistorikk(
            setOf(bostedsadresse1, bostedsadresse2),
            null, null, Folkeregisteridentifikator("identNr"),
            setOf(Folkeregisterpersonstatus(Personstatuser.UDEFINERT, "ny status fra PDL", Master.PDL.name, Master.PDL.name, null, false)),
            KjoennType.UKJENT,
            setOf(kontaktadresse1, kontaktadresse2), Navn("Ola", "Oops", "King"),
            setOf(oppholdsadresse1, oppholdsadresse2), setOf(
                Sivilstand(
                    Sivilstandstype.REGISTRERT_PARTNER, null, "relatertVedSivilstandID", LocalDate.MIN,
                    LocalDate.EPOCH, "PDL", "kilde", false
                ),
                Sivilstand(
                    Sivilstandstype.UDEFINERT, "Udefinert type", "relatertVedSivilstandID", LocalDate.MIN,
                    LocalDate.EPOCH, "PDL", "kilde", false
                )
            ),
            setOf(statsborgerskap1, statsborgerskap2, statsborgerskap3)
        )
    }

    fun lagBarn() = Familiemedlem(
        Folkeregisteridentifikator("fnrBarn"), Navn("barn", null, "etternavn"),
        Familierelasjon.BARN, Foedsel(LocalDate.now().minusYears(42), null, null, null),
        Folkeregisteridentifikator("fnrAnnenForelder"), "felles", null
    )

    fun lagRelatertVedsivilstand() = Familiemedlem(
        Folkeregisteridentifikator("fnr"), Navn("fornavn", null, "etternavn"),
        Familierelasjon.RELATERT_VED_SIVILSTAND, Foedsel(LocalDate.MIN, null, null, null), null, "ukjent",
        Sivilstand(
            Sivilstandstype.GIFT, null, "relatertVedSivilstandID", LocalDate.MIN, null, "Dolly", "PDL",
            false
        )
    )

    fun lagDonaldDuckPersondata() = Personopplysninger(
        emptyList(),
        Bostedsadresse(
            lagStrukturertAdresse(), "",
            LocalDate.now().minusYears(2), LocalDate.now().plusYears(2), Master.PDL.name, "", false
        ),
        null,
        emptySet(),
        null,
        null,
        KjoennType.UKJENT,
        listOf(lagKontaktadresse()),
        Navn("Donald", null, "Duck"),
        listOf(lagOppholdsadresse()),
        emptyList()
    )

    private fun lagOppholdsadresse() = Oppholdsadresse(
        lagStrukturertAdresse(),
        null,
        LocalDate.now().minusYears(2),
        LocalDate.now().plusYears(2),
        Master.PDL.name,
        Master.PDL.name,
        LocalDateTime.now(),
        false
    )

    private fun lagKontaktadresse() = Kontaktadresse(
        lagStrukturertAdresse(),
        null,
        null,
        LocalDate.now().minusYears(2),
        LocalDate.now().plusYears(2),
        Master.PDL.name,
        Master.PDL.name,
        LocalDateTime.now(),
        false
    )

    private fun lagStrukturertAdresse() = StrukturertAdresse(
        ADRESSELINJE_1_BRUKER,
        HUSNUMMER,
        POSTNR_BRUKER,
        POSTSTED_BRUKER,
        REGION,
        Landkoder.SE.name
    )
}
