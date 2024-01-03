package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode
import no.nav.melosys.service.kontroll.regler.PersonRegler.erPersonDød
import no.nav.melosys.service.kontroll.regler.PersonRegler.personBosattINorge
import no.nav.melosys.service.kontroll.regler.PersonRegler.personBosattINorgeIPeriode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import java.util.List


class PersonReglerTest {

    @Test
    fun personDød_personErDød_true() {
        val personDokument = PersonDokument()
        personDokument.dødsdato = LocalDate.now()
        Assertions.assertThat(erPersonDød(personDokument)).isTrue()
    }

    @Test
    fun personBosattINorge_bosattINorgeIPerioden_true() {
        val bostedsadressePeriode = BostedsadressePeriode()
        bostedsadressePeriode.periode = Periode(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 12, 20))
        bostedsadressePeriode.bostedsadresse = Bostedsadresse()
        bostedsadressePeriode.bostedsadresse.land = Land(Land.NORGE)

        val bostedsadressePeriodeList = List.of(bostedsadressePeriode)

        val bostedsadresse = no.nav.melosys.domain.person.adresse.Bostedsadresse(
            StrukturertAdresse(),
            null,
            LocalDate.of(2024, 1, 2),
            LocalDate.of(2024, 12, 20),
            "",
            "",
            false
        )
        bostedsadresse.strukturertAdresse.landkode = "NO"


        val periodeFra = LocalDate.of(2023, 3, 23)
        val periodeTil = LocalDate.of(2023, 3, 25)

        val personenHarBostedINorgeIPerioden =
            personBosattINorgeIPeriode(bostedsadressePeriodeList, Optional.of(bostedsadresse), Collections.emptyList(), Collections.emptyList(), periodeFra, periodeTil)

        Assertions.assertThat(personenHarBostedINorgeIPerioden).isTrue()
    }


    @Test
    fun personBosattINorge_bosattINorgeIPerioden_false() {
        val bostedsadressePeriode = BostedsadressePeriode()
        bostedsadressePeriode.periode = Periode(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 12, 20))
        bostedsadressePeriode.bostedsadresse = Bostedsadresse()
        bostedsadressePeriode.bostedsadresse.land = Land(Land.NORGE)


        val bostedsadressePeriodeList = List.of(bostedsadressePeriode)

        val periodeFra = LocalDate.of(2022, 2, 1)
        val periodeTil = LocalDate.of(2023, 12, 1)

        val personenHarBostedINorgeIPerioden = personBosattINorgeIPeriode(bostedsadressePeriodeList, Optional.empty(), Collections.emptyList(), Collections.emptyList(), periodeFra, periodeTil)

        Assertions.assertThat(personenHarBostedINorgeIPerioden).isFalse()
    }

    @Test
    fun personDød_ingenDødsdato_false() {
        Assertions.assertThat(erPersonDød(PersonDokument())).isFalse()
    }

    @Test
    fun personBosattINorge_bosattINorge_true() {
        val personDokument = PersonDokument()
        personDokument.dødsdato = LocalDate.now()
        personDokument.bostedsadresse = Bostedsadresse()
        personDokument.bostedsadresse.land = Land(Land.NORGE)
        Assertions.assertThat(personBosattINorge(personDokument)).isTrue()
    }

    @Test
    fun personBosattINorge_ikkeBosattINorge_false() {
        val personDokument = PersonDokument()
        personDokument.dødsdato = LocalDate.now()
        personDokument.bostedsadresse = Bostedsadresse()
        personDokument.bostedsadresse.land = Land(Land.SVEITS)
        Assertions.assertThat(personBosattINorge(personDokument)).isFalse()
    }

    @Test
    fun personBosattINorge_ingenBostedsadresse_false() {
        Assertions.assertThat(personBosattINorge(PersonDokument())).isFalse()
    }
}
