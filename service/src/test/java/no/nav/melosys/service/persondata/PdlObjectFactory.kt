package no.nav.melosys.service.persondata

import no.nav.melosys.integrasjon.pdl.dto.Endring
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPHOER
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT
import no.nav.melosys.integrasjon.pdl.dto.Folkeregistermetadata
import no.nav.melosys.integrasjon.pdl.dto.Metadata
import no.nav.melosys.integrasjon.pdl.dto.person.*
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Matrikkeladresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse
import java.time.LocalDate
import java.time.LocalDateTime

object PdlObjectFactory {
    private val METADATA = lagMetadata()
    private val FOLKEREGISTERMETADATA = lagFolkeregistermetadata()

    fun metadata(): Metadata {
        return METADATA
    }

    fun folkeregistermetadata(): Folkeregistermetadata {
        return FOLKEREGISTERMETADATA
    }

    fun lagPerson(): Person {
        return Person(
            setOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadata())),
            setOf(
                lagUtenlandskBostedsadresse("adresse utland", LocalDateTime.MIN),
                lagNorskBostedsadresse("gata", LocalDateTime.MAX)
            ),
            setOf(Doedsfall(LocalDate.MAX, metadata())),
            setOf(Foedested("NOR", "fødested", metadata())),
            setOf(Foedselsdato(LocalDate.EPOCH, 1970, metadata())),
            setOf(Folkeregisteridentifikator("IdNr", metadata())),
            setOf(Folkeregisterpersonstatus("ikkeBosatt", metadata(), folkeregistermetadata())),
            setOf(
                ForelderBarnRelasjon("barnIdent", Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR, metadata()),
                ForelderBarnRelasjon("forelderIdent", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN, metadata())
            ),
            setOf(Foreldreansvar("felles", metadata())),
            setOf(
                Kjoenn(KjoennType.UKJENT, lagMetadata(LocalDateTime.MIN)),
                Kjoenn(KjoennType.UKJENT, lagMetadata(LocalDateTime.MAX))
            ),
            emptyList(),
            setOf(Navn("fornavn", "mellomnavn", "etternavn", metadata())),
            emptyList(),
            setOf(lagSivilstand()),
            setOf(
                Statsborgerskap(
                    "AIA", null,
                    LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), lagMetadata(LocalDateTime.MIN)
                ),
                Statsborgerskap(
                    "NOR", LocalDate.parse("2021-05-08"), null,
                    null, lagMetadata(LocalDateTime.MAX)
                )
            ),
            null
        )
    }

    @JvmStatic
    @JvmOverloads
    fun lagSivilstand(relatertVedSivilstandId: String = "relatertVedSivilstandID"): Sivilstand {
        return Sivilstand(
            Sivilstandstype.GIFT, relatertVedSivilstandId, LocalDate.MIN, LocalDate.EPOCH,
            lagMetadata()
        )
    }

    @JvmStatic
    fun lagBostedsadresseMedMatrikkelAdresse(): Bostedsadresse {
        return Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            null,
            null,
            Matrikkeladresse("tilleggsnavn", "4321"),
            null,
            null,
            metadata()
        )
    }

    @JvmStatic
    @JvmOverloads
    fun lagNorskBostedsadresse(gatenavn: String = "Kirkegata", registrertDato: LocalDateTime? = null): Bostedsadresse {
        return if (registrertDato == null) {
            Bostedsadresse(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-05-05T00:00:00"),
                "Kari Hansen",
                Vegadresse(
                    "Kirkegata",
                    "12",
                    "B",
                    "Storgården",
                    "1234"
                ),
                null,
                null,
                null,
                metadata()
            )
        } else {
            Bostedsadresse(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-05-05T00:00:00"),
                "Kari Hansen",
                Vegadresse(
                    gatenavn,
                    "12",
                    "B",
                    "Storgården",
                    "1234"
                ),
                null,
                null,
                null,
                lagMetadata(registrertDato)
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun lagUtenlandskBostedsadresse(gatenavn: String = "adressenavnNummer", registrertDato: LocalDateTime? = null): Bostedsadresse {
        return if (registrertDato == null) {
            Bostedsadresse(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-05-05T00:00:00"),
                "Kari Hansen",
                null,
                null,
                UtenlandskAdresse(
                    "adressenavnNummer",
                    "bygningEtasjeLeilighet",
                    "P.O.Box 1234 Place",
                    "SE-12345",
                    "Haworth",
                    "Yorkshire",
                    "SWE"
                ),
                null,
                metadata()
            )
        } else {
            Bostedsadresse(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-05-05T00:00:00"),
                "Kari Hansen",
                null,
                null,
                UtenlandskAdresse(
                    gatenavn,
                    "bygningEtasjeLeilighet",
                    "P.O.Box 1234 Place",
                    "SE-12345",
                    "Haworth",
                    "Yorkshire",
                    "SWE"
                ),
                null,
                lagMetadata(registrertDato)
            )
        }
    }

    @JvmStatic
    fun lagUgyldigBostedsadresse(): Bostedsadresse {
        return Bostedsadresse(
            LocalDateTime.parse("2020-01-01T00:00:00"),
            LocalDateTime.parse("2020-05-05T00:00:00"),
            null,
            null,
            Matrikkeladresse("", "4321"),
            null,
            null,
            lagMetadataOpphørt()
        )
    }

    @JvmStatic
    @JvmOverloads
    fun lagMetadata(registrertDato: LocalDateTime = LocalDateTime.parse("2021-05-07T10:04:52")): Metadata {
        return Metadata(
            "PDL", false,
            listOf(Endring(OPPRETT, registrertDato, "Dolly"))
        )
    }

    private fun lagMetadataOpphørt(): Metadata {
        return Metadata(
            "FREG", true, listOf(
                Endring(OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "Freg"),
                Endring(OPPHOER, LocalDateTime.parse("2023-05-07T10:04:52"), "Bruker")
            )
        )
    }

    @JvmStatic
    fun lagFolkeregistermetadata(): Folkeregistermetadata {
        return Folkeregistermetadata(
            LocalDateTime.parse("2021-04-07T10:04:52"),
            LocalDateTime.parse("2021-05-07T10:04:52"),
            LocalDateTime.parse("2021-06-07T10:04:52"),
            null,
            null
        )
    }
}
