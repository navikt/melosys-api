package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.service.kontroll.regler.PersonRegler.NORGE_ISO2_LANDKODE
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class PersonReglerTest {

    @Test
    fun `person er død gir true`() {
        PersonRegler.erPersonDød(persondata = PersonDokument().apply {
            dødsdato = LocalDate.now()
        }) shouldBe true
    }

    @Test
    fun `bosatt i Norge i perioden gir true`() {
        PersonRegler.personBosattINorgeIPeriode(
            bostedsadressePerioder = listOf(
                BostedsadressePeriode(
                    periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 12, 20)),
                    endringstidspunkt = null,
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land(Land.NORGE)
                    }
                )
            ),
            bostedsadresseOptional = Optional.of(
                no.nav.melosys.domain.person.adresse.Bostedsadresse(
                    StrukturertAdresse().apply { this.landkode = "NO" },
                    null,
                    LocalDate.of(2024, 1, 2),
                    LocalDate.of(2024, 12, 20),
                    "",
                    "",
                    false
                )
            ),
            historiskBostedadresser = emptyList(),
            historiskOppholdsadresser = emptyList(),
            periodeFra = LocalDate.of(2023, 3, 23),
            periodeTil = LocalDate.of(2023, 3, 25)
        ) shouldBe true
    }

    @Test
    fun `ikke bosatt i Norge i perioden gir false`() {
        PersonRegler.personBosattINorgeIPeriode(
            bostedsadressePerioder = listOf(
                BostedsadressePeriode(
                    periode = Periode(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 12, 20)),
                    endringstidspunkt = null,
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land(Land.NORGE)
                    }
                )
            ),
            bostedsadresseOptional = Optional.empty(),
            historiskBostedadresser = emptyList(),
            historiskOppholdsadresser = emptyList(),
            periodeFra = LocalDate.of(2022, 2, 1),
            periodeTil = LocalDate.of(2023, 12, 1)
        ) shouldBe false
    }

    @Test
    fun `oppholdsadresse i Norge i perioden, men ikke bosted adresse gir false`() {
        PersonRegler.personBosattINorgeIPeriode(
            emptyList(),
            Optional.empty(),
            emptyList(),
            listOf(
                Oppholdsadresse(
                    StrukturertAdresse().apply { this.landkode = NORGE_ISO2_LANDKODE },
                    null,
                    LocalDate.of(2024, 1, 2),
                    LocalDate.of(2024, 12, 20),
                    "",
                    "",
                    LocalDateTime.now(),
                    false
                )
            ),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 3, 25)
        ) shouldBe false
    }

    @Test
    fun `person uten dødsdato gir false`() {
        PersonRegler.erPersonDød(PersonDokument()) shouldBe false
    }

    @Test
    fun `bosatt i Norge gir true`() {
        PersonRegler.personBosattINorge(persondata = PersonDokument().apply {
            dødsdato = LocalDate.now()
            bostedsadresse = Bostedsadresse().apply {
                land = Land(Land.NORGE)
            }
        }) shouldBe true
    }

    @Test
    fun `bosatt i Sveits gir false`() {
        PersonRegler.personBosattINorge(persondata = PersonDokument().apply {
            dødsdato = LocalDate.now()
            bostedsadresse = Bostedsadresse().apply {
                land = Land(Land.SVEITS)
            }
        }) shouldBe false
    }

    @Test
    fun `ingen bostedsadresse gir false`() {
        PersonRegler.personBosattINorge(persondata = PersonDokument()) shouldBe false
    }
}
