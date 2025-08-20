package no.nav.melosys.domain.person

import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Personstatuser
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import java.time.LocalDate
import java.time.LocalDateTime

object PersonopplysningerObjectFactory {

    fun lagPersonopplysninger(): Personopplysninger =
        Personopplysninger(
            emptyList(), lagBostedsadresse(), null, emptySet(),
            lagFødesel(), null, lagKjønn(), lagKontaktadresser(), lagNavn(), lagOppholdsadresser(),
            lagStatsborgerskap()
        )

    private fun lagFødesel() = Foedsel(LocalDate.EPOCH, null, null, null)

    private fun lagKjønn() = KjoennType.UKJENT

    private fun lagBostedsadresse() = Bostedsadresse(
        StrukturertAdresse("gatenavnFraBostedsadresse", "3", "1234", "Oslo", "Norge", "NO"),
        null, null, null, null, null, false
    )

    fun lagKontaktadresser(): Collection<Kontaktadresse> = setOf(
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

    private fun lagOppholdsadresser(): Collection<Oppholdsadresse> = setOf(
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

    private fun lagStrukturertAdresse(gatenavn: String) =
        StrukturertAdresse(gatenavn, null, "0123", "Poststed", null, "NO")

    private fun lagNavn() = Navn("Ola", null, "Nordmann")

    private fun lagStatsborgerskap(): Collection<Statsborgerskap> = listOf(
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
            setOf(
                Folkeregisterpersonstatus(
                    Personstatuser.UDEFINERT, "ny status fra PDL", Master.PDL.name, Master.PDL.name, null, false
                )
            ),
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
}