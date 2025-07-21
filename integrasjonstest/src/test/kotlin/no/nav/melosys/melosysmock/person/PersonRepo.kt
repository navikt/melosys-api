package no.nav.melosys.melosysmock.person

import java.time.LocalDate

object PersonRepo {

    val repo = mutableMapOf<String, Person>()
    val aktørIdRepo = mutableMapOf<String, Person>()

    init {
        leggTilPerson(
            Person(
                ident = "30056928150",
                fornavn = "KARAFFEL",
                etternavn = "TRIVIELL",
                foedselsdato = LocalDate.of(1969, 5, 30),
                statsborgerskap = listOf("NOR"),
                kjønn = "M",
                aktørId = "1111111111111"
            )
        )
        leggTilPerson(
            Person(
                ident = "21075114491",
                fornavn = "BRÅKETE",
                etternavn = "GYNGEHEST",
                foedselsdato = LocalDate.of(1975, 7, 21),
                statsborgerskap = listOf("NOR"),
                kjønn = "M",
                aktørId = "2222222222222"
            )
        )
        leggTilPerson(
            Person(
                ident = "77777777777",
                fornavn = "JUNIOR",
                etternavn = "TRIVIELL",
                foedselsdato = LocalDate.of(1979, 6, 23),
                statsborgerskap = listOf("NOR"),
                kjønn = "M",
                aktørId = "3333333333333"
            )
        )
        leggTilPerson(
            Person(
                ident = "12028536819",
                fornavn = "ROTAT",
                etternavn = "KAFFE",
                foedselsdato = LocalDate.of(1974, 2, 13),
                statsborgerskap = listOf("NOR"),
                kjønn = "F",
                aktørId = "4444444444444"
            )
        )
    }

    fun leggTilPerson(person: Person) {
        repo[person.ident] = person
        aktørIdRepo[person.aktørId] = person
    }

    fun finnVedIdent(ident: String): Person? = repo[ident] ?: aktørIdRepo[ident]
}

data class Person(
    val ident: String,
    val fornavn: String,
    val etternavn: String,
    val foedselsdato: LocalDate,
    val statsborgerskap: List<String>,
    val kjønn: String,
    val aktørId: String,
    val bostedsadresse: Adresse? = Adresse(gatenavn = "bosted gata 3", landkode = "NOR"),
    val oppholdsadresse: Adresse? = Adresse(gatenavn = "opphold gata 1", landkode = "NOR"),
    val kontaktadresse: Adresse? = Adresse(gatenavn = "kontakt gata 1", landkode = "SWE"),
)

data class Adresse(
    val gatenavn: String = "Gate 1",
    val husnummer: String = "42",
    val husbokstav: String = "B",
    val postnummer: String = "0001",
    val landkode: String = "NOR",
)
