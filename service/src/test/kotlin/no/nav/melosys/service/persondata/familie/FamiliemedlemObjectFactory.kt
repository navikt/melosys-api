package no.nav.melosys.service.persondata.familie

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.pdl.dto.Endring
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.KORRIGER
import no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT
import no.nav.melosys.integrasjon.pdl.dto.Metadata
import no.nav.melosys.integrasjon.pdl.dto.person.*
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FamiliemedlemObjectFactory {
    val IDENT_PERSON_GIFT = "21075114491"
    val PERSON_GIFT_FORNAVN = "BRÅKETE"
    val IDENT_PERSON_GIFT_HISTORISK = "12028536819"
    val IDENT_BARN = "66628536666"
    val NULL_IDENT_NÅR_SKILT: String? = null
    val BEHANDLING_ID = 1L

    fun lagHovedpersonMedBarn(): Person {
        return Person(
            emptySet(),
            setOf(lagNorskBostedsadresse()),
            emptySet(),
            setOf(lagFødselsstedForVoksen()),
            setOf(lagFødselsdatoForVoksen()),
            setOf(Folkeregisteridentifikator(FagsakTestFactory.BRUKER_AKTØR_ID, lagAktivMetadata())),
            emptySet(),
            lagForelderBarnRelasjoner(),
            emptySet(),
            setOf(Kjoenn(KjoennType.MANN, lagAktivMetadata())),
            emptySet(),
            setOf(Navn("KARAFFEL", "", "TRIVIELL", lagAktivMetadata())),
            emptySet(),
            lagSivilstandForHovedperson(),
            setOf(Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            emptySet()
        )
    }

    fun lagHovedpersonMedBarn_medKorrigertGiftSeparertSkiltPåSammeDato(): Person {
        return Person(
            emptySet(),
            setOf(lagNorskBostedsadresse()),
            emptySet(),
            setOf(lagFødselsstedForVoksen()),
            setOf(lagFødselsdatoForVoksen()),
            setOf(Folkeregisteridentifikator(FagsakTestFactory.BRUKER_AKTØR_ID, lagAktivMetadata())),
            emptySet(),
            lagForelderBarnRelasjoner(),
            emptySet(),
            setOf(Kjoenn(KjoennType.MANN, lagAktivMetadata())),
            emptySet(),
            setOf(Navn("KARAFFEL", "", "TRIVIELL", lagAktivMetadata())),
            emptySet(),
            lagSivilstandForHovedperson_korrigertMedSammeDato(),
            setOf(Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            emptySet()
        )
    }

    fun lagHovedperson(): Person {
        return Person(
            emptySet(),
            setOf(lagNorskBostedsadresse()),
            emptySet(),
            setOf(lagFødselsstedForVoksen()),
            setOf(lagFødselsdatoForVoksen()),
            setOf(Folkeregisteridentifikator(FagsakTestFactory.BRUKER_AKTØR_ID, lagAktivMetadata())),
            emptySet(),
            emptySet(),
            emptySet(),
            setOf(Kjoenn(KjoennType.MANN, lagAktivMetadata())),
            emptySet(),
            setOf(Navn("KARAFFEL", "", "TRIVIELL", lagAktivMetadata())),
            emptySet(),
            lagSivilstandForHovedperson(),
            setOf(Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            emptySet()
        )
    }

    fun lagSivilstandForHovedperson(): Set<Sivilstand> {
        return setOf(
            Sivilstand(
                Sivilstandstype.GIFT,
                IDENT_PERSON_GIFT_HISTORISK,
                LocalDate.of(2020, 5, 4),
                null,
                lagAktivMetadata(toLocalDateTime("2020-04-27T14:57:56"))
            ),
            Sivilstand(
                Sivilstandstype.SEPARERT,
                IDENT_PERSON_GIFT_HISTORISK,
                LocalDate.of(2020, 6, 4),
                null,
                lagHistoriskMetadata(toLocalDateTime("2020-04-28T13:28:43"))
            ),
            Sivilstand(
                Sivilstandstype.SKILT,
                NULL_IDENT_NÅR_SKILT,
                LocalDate.of(2020, 7, 4),
                null,
                lagHistoriskMetadata(toLocalDateTime("2020-05-04T12:20:45"))
            ),
            Sivilstand(
                Sivilstandstype.GIFT,
                IDENT_PERSON_GIFT,
                LocalDate.of(2023, 1, 1),
                null,
                lagAktivMetadata(toLocalDateTime("2022-12-05T08:32:21"))
            )
        )
    }

    private fun lagSivilstandForHovedperson_korrigertMedSammeDato(): Set<Sivilstand> {
        val korrigeringsTid = toLocalDateTime("2023-04-27T14:57:56")
        return setOf(
            Sivilstand(
                Sivilstandstype.GIFT,
                IDENT_PERSON_GIFT_HISTORISK,
                LocalDate.of(2020, 5, 4),
                null,
                lagAktivMetadataMedKorrigering(toLocalDateTime("2020-04-27T14:57:56"), korrigeringsTid)
            ),
            Sivilstand(
                Sivilstandstype.SEPARERT,
                IDENT_PERSON_GIFT_HISTORISK,
                LocalDate.of(2020, 6, 4),
                null,
                lagHistoriskMetadataMedKorrigering(toLocalDateTime("2020-04-28T13:28:43"), korrigeringsTid)
            ),
            Sivilstand(
                Sivilstandstype.SKILT,
                NULL_IDENT_NÅR_SKILT,
                LocalDate.of(2020, 7, 4),
                null,
                lagHistoriskMetadataMedKorrigering(toLocalDateTime("2020-05-04T12:20:45"), korrigeringsTid)
            ),
            Sivilstand(
                Sivilstandstype.GIFT,
                IDENT_PERSON_GIFT,
                LocalDate.of(2023, 1, 1),
                null,
                lagAktivMetadata(toLocalDateTime("2022-12-05T08:32:21"))
            )
        )
    }

    fun lagPersonGift(): Person {
        return Person(
            emptySet(),
            setOf(lagNorskBostedsadresse()),
            emptySet(),
            setOf(lagFødselsstedForVoksen()),
            setOf(lagFødselsdatoForVoksen()),
            setOf(Folkeregisteridentifikator(IDENT_PERSON_GIFT, lagAktivMetadata())),
            emptySet(),
            emptySet(),
            emptySet(),
            setOf(Kjoenn(KjoennType.KVINNE, lagAktivMetadata())),
            emptySet(),
            setOf(Navn(PERSON_GIFT_FORNAVN, "", "GYNGEHEST", lagAktivMetadata())),
            emptySet(),
            setOf(
                Sivilstand(
                    Sivilstandstype.GIFT,
                    FagsakTestFactory.BRUKER_AKTØR_ID,
                    LocalDate.of(2019, 8, 3),
                    null,
                    lagAktivMetadata(toLocalDateTime("2020-12-05T08:32:21"))
                )
            ),
            setOf(Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            emptySet()
        )
    }

    fun lagBehandling(): Behandling {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medFagsak(FagsakTestFactory.builder().medBruker().build())
            .build()
    }

    fun lagNorskBostedsadresse(): Bostedsadresse {
        return Bostedsadresse(
            LocalDateTime.of(2015, 1, 1, 0, 0, 0),
            LocalDateTime.of(2999, 5, 5, 0, 0, 0),
            "Kongen",
            Vegadresse(
                "Slottsplassen 1",
                "1",
                null,
                "",
                "0010"
            ),
            null,
            null,
            null,
            lagAktivMetadata(LocalDateTime.of(2022, 6, 26, 13, 37, 0))
        )
    }

    fun lagAktivMetadata(registrertDato: LocalDateTime = LocalDateTime.of(1950, 1, 11, 12, 0, 0)): Metadata {
        return Metadata(
            "PDL", false,
            listOf(Endring(OPPRETT, registrertDato, "Z123456"))
        )
    }

    fun lagAktivMetadataMedKorrigering(
        registrertDato: LocalDateTime,
        korrigeringDato: LocalDateTime
    ): Metadata {
        return Metadata(
            "PDL", false,
            listOf(
                Endring(OPPRETT, registrertDato, "Z123456"),
                Endring(KORRIGER, korrigeringDato, "Z123456")
            )
        )
    }

    fun lagHistoriskMetadataMedKorrigering(
        registrertDato: LocalDateTime,
        korrigeringDato: LocalDateTime
    ): Metadata {
        return Metadata(
            "PDL", true,
            listOf(
                Endring(OPPRETT, registrertDato, "Z123456"),
                Endring(KORRIGER, korrigeringDato, "Z123456")
            )
        )
    }

    fun lagHistoriskMetadata(registrertDato: LocalDateTime): Metadata {
        return Metadata(
            "PDL", true,
            listOf(Endring(OPPRETT, registrertDato, "Z123456"))
        )
    }

    private fun toLocalDateTime(localDateTime: String): LocalDateTime {
        return LocalDateTime.parse(localDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun lagForelderBarnRelasjoner(): Set<ForelderBarnRelasjon> {
        return setOf(
            ForelderBarnRelasjon(IDENT_BARN, Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR, lagAktivMetadata()),
            ForelderBarnRelasjon("forelderIdent", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN, lagAktivMetadata())
        )
    }

    private fun lagFødselsstedForVoksen(): Foedested {
        return Foedested("NOR", "Oslo", lagAktivMetadata())
    }

    private fun lagFødselsdatoForVoksen(): Foedselsdato {
        return Foedselsdato(LocalDate.of(1950, 1, 1), 1950, lagAktivMetadata())
    }
}
