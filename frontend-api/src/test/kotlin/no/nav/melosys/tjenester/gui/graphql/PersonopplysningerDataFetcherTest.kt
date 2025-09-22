package no.nav.melosys.tjenester.gui.graphql

import graphql.execution.ExecutionStepInfo
import graphql.schema.DataFetchingEnvironment
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Personstatuser
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.tjenester.gui.graphql.dto.FolkeregisterpersonstatusDto
import no.nav.melosys.tjenester.gui.graphql.dto.NavnDto
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.function.Consumer

@ExtendWith(MockKExtension::class)
class PersonopplysningerDataFetcherTest {
    private val kodeverkService = mockk<KodeverkService>()
    private val persondataFasade = mockk<PersondataFasade>()
    private val dataFetchingEnvironment = mockk<DataFetchingEnvironment>()
    private val executionStepInfo = mockk<ExecutionStepInfo>()

    @Test
    fun `get med BehandlingID returner data`() {
        val personopplysningerDataFetcher = PersonopplysningerDataFetcher(kodeverkService, persondataFasade)

        every { dataFetchingEnvironment.executionStepInfo } returns executionStepInfo
        every { dataFetchingEnvironment.getArgument<String>("ident") } returns null
        every { executionStepInfo.parent } returns executionStepInfo
        every { executionStepInfo.getArgument<Long>("behandlingID") } returns 1L
        every { persondataFasade.hentPersonMedHistorikk(any<Long>()) } returns lagPersonMedHistorikk()
        every { kodeverkService.dekod(eq(FellesKodeverk.LANDKODER_ISO2), any<String>()) } returns "My country"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA") } returns "Testland A"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB") } returns "Testland B"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC") } returns "Testland C"


        val personopplysninger = personopplysningerDataFetcher.get(dataFetchingEnvironment)


        assertFetched(personopplysninger)
    }

    @Test
    fun `get med ident returner data`() {
        val personopplysningerDataFetcher = PersonopplysningerDataFetcher(kodeverkService, persondataFasade)

        every { dataFetchingEnvironment.executionStepInfo } returns null
        every { dataFetchingEnvironment.getArgument<String>("ident") } returns "Z990077"
        every { persondataFasade.hentPersonMedHistorikk("Z990077") } returns lagPersonMedHistorikk()
        every { kodeverkService.dekod(eq(FellesKodeverk.LANDKODER_ISO2), any<String>()) } returns "My country"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA") } returns "Testland A"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB") } returns "Testland B"
        every { kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC") } returns "Testland C"


        val personopplysninger = personopplysningerDataFetcher.get(dataFetchingEnvironment)


        assertFetched(personopplysninger)
    }

    private fun assertFetched(personopplysninger: PersonopplysningerDto) {
        personopplysninger.run {
            bostedsadresser().map { it.adresse.gatenavn } shouldContainExactlyInAnyOrder listOf("gate1", "gate2")
            bostedsadresser().map { it.adresse.husnummerEtasjeLeilighet } shouldContainExactlyInAnyOrder listOf("42 C", null)
            bostedsadresser().map { it.master } shouldContainExactlyInAnyOrder listOf("NAV (PDL)", "")
            foedsel().foedested() shouldBe "Oslo"
            foedsel().foedeland() shouldBe "NO"
            foedsel().foedselsdato() shouldBe LocalDate.MIN
            folkeregisteridentifikator() shouldBe "identNr"
            folkeregisterpersonstatuser() shouldContainExactly listOf(
                FolkeregisterpersonstatusDto(
                    Personstatuser.UDEFINERT.kode,
                    "ny status fra PDL",
                    "NAV (PDL)",
                    Master.PDL.name,
                    LocalDate.parse("2019-11-18"),
                    false
                )
            )
            kjoenn() shouldBe KjoennType.UKJENT
            kontaktadresser().map { it.master } shouldContainExactlyInAnyOrder listOf("NAV (PDL)", "")
            navn() shouldBe NavnDto("Ola", "Oops", "King")
            oppholdsadresser().map { it.adresse.gatenavn } shouldContainExactlyInAnyOrder listOf("opphold 1", "opphold 2")
            oppholdsadresser().map { it.adresse.tilleggsnavn } shouldContainExactlyInAnyOrder listOf("tilleggOpphold", null)
            oppholdsadresser().map { it.master } shouldContainExactlyInAnyOrder listOf("NAV (PDL)", "")

            // Sivilstand assertions
            val sivilstandValues = sivilstand().flatMap { sivilstand ->
                listOf(
                    sivilstand.type(), sivilstand.relatertVedSivilstand(), sivilstand.gyldigFraOgMed(),
                    sivilstand.bekreftelsesdato(), sivilstand.master(), sivilstand.kilde(), sivilstand.erHistorisk()
                )
            }
            sivilstandValues shouldContainExactly listOf(
                "Udefinert type", "relatertVedSivilstandID", LocalDate.MIN.plusDays(1), LocalDate.EPOCH, "NAV (PDL)", "kilde", false,
                "Registrert partner", "relatertVedSivilstandID", LocalDate.MIN, LocalDate.EPOCH, "NAV (PDL)", "kilde", false
            )
        }

        val statsborgerskapErSortert = Consumer<PersonopplysningerDto> { personopplysningerDto ->
            personopplysningerDto.run {
                statsborgerskap()[0].land() shouldBe "Testland C"
                statsborgerskap()[0].master() shouldBe "NAV (PDL)"
                statsborgerskap()[1].land() shouldBe "Testland A"
                statsborgerskap()[2].land() shouldBe "Testland B"
            }
        }
        personopplysninger.shouldBeInstanceOf<PersonopplysningerDto>()
        statsborgerskapErSortert.accept(personopplysninger)
    }

    private fun lagPersonMedHistorikk(): PersonMedHistorikk {
        val bostedsadresse_1 = Bostedsadresse(
            StrukturertAdresse("gate1", "42 C", null, null, null, null),
            null, null, null, "PDL", null, false
        )
        val bostedsadresse_2 = Bostedsadresse(
            StrukturertAdresse("gate2", null, null, null, null, null),
            null, null, null, null, null, true
        )

        val kontaktadresse_1 = Kontaktadresse(
            StrukturertAdresse("kontakt 1", null, null, null, null, null), null, null, null, null, "PDL", null, null,
            false
        )
        val kontaktadresse_2 = Kontaktadresse(
            null,
            SemistrukturertAdresse("kontakt 2", "linje 2", null, null, "1234", "By", "IT"), null, null, null, null,
            null, null, false
        )

        val oppholdsadresse_1 = Oppholdsadresse(
            StrukturertAdresse("tilleggOpphold", "opphold 1", null, null, null, null, null, null), null, null, null,
            "PDL", null, null, false
        )
        val oppholdsadresse_2 = Oppholdsadresse(
            StrukturertAdresse("opphold 2", null, null, null, null, null), null, null, null, null, null, null,
            true
        )

        val foedsel = Foedsel(LocalDate.MIN, LocalDate.MIN.year, "NOR", "Oslo")

        val statsborgerskap_1 = Statsborgerskap(
            "AAA", null, LocalDate.parse("2009-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
        )
        val statsborgerskap_2 = Statsborgerskap(
            "BBB", null, LocalDate.parse("1979-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
        )
        val statsborgerskap_3 = Statsborgerskap(
            "CCC", null, null, LocalDate.parse("1980-11-18"), "PDL",
            "Dolly", false
        )

        return PersonMedHistorikk(
            setOf(bostedsadresse_1, bostedsadresse_2),
            null,
            foedsel,
            Folkeregisteridentifikator("identNr"),
            setOf(
                Folkeregisterpersonstatus(
                    Personstatuser.UDEFINERT,
                    "ny status fra PDL",
                    Master.PDL.name,
                    Master.PDL.name,
                    LocalDate.parse("2019-11-18"),
                    false
                )
            ),
            KjoennType.UKJENT,
            setOf(kontaktadresse_1, kontaktadresse_2),
            Navn("Ola", "Oops", "King"),
            setOf(oppholdsadresse_1, oppholdsadresse_2),
            setOf(
                Sivilstand(
                    Sivilstandstype.REGISTRERT_PARTNER, null, "relatertVedSivilstandID", LocalDate.MIN, LocalDate.EPOCH, "PDL", "kilde", false
                ),
                Sivilstand(
                    Sivilstandstype.UDEFINERT,
                    "Udefinert type",
                    "relatertVedSivilstandID",
                    LocalDate.MIN.plusDays(1),
                    LocalDate.EPOCH,
                    "PDL",
                    "kilde",
                    false
                )
            ),
            setOf(statsborgerskap_1, statsborgerskap_2, statsborgerskap_3)
        )
    }
}
