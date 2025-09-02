package no.nav.melosys.tjenester.gui.graphql

import graphql.execution.ExecutionStepInfo
import graphql.schema.DataFetchingEnvironment
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.junit5.MockKExtension
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.familie.Familiemedlem
import no.nav.melosys.domain.person.familie.Familierelasjon
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class FamiliemedlemmerDataFetcherTest {
    private val persondataFasade = mockk<PersondataFasade>()
    private val dataFetchingEnvironment = mockk<DataFetchingEnvironment>()
    private val executionStepInfo = mockk<ExecutionStepInfo>()

    @Test
    fun `get med behandlingID skal returnere data`() {
        val familiemedlemmerDataFetcher = FamiliemedlemmerDataFetcher(persondataFasade)
        val medlemmer = setOf(lagBarn(), lagRelatertVedsivilstand())
        every { dataFetchingEnvironment.executionStepInfo } returns executionStepInfo
        every { executionStepInfo.parent } returns executionStepInfo
        every { executionStepInfo.getArgument<Long>("behandlingID") } returns 1L
        every { executionStepInfo.getArgument<String>("ident") } returns null
        every { persondataFasade.hentFamiliemedlemmerFraBehandlingID(1L) } returns medlemmer


        val familieDtoListe = familiemedlemmerDataFetcher.get(dataFetchingEnvironment)


        assertFetched(familieDtoListe)
    }

    @Test
    fun `get med ident skal returnere data`() {
        val familiemedlemmerDataFetcher = FamiliemedlemmerDataFetcher(persondataFasade)
        val medlemmer = setOf(lagBarn(), lagRelatertVedsivilstand())
        every { dataFetchingEnvironment.executionStepInfo } returns executionStepInfo
        every { executionStepInfo.parent } returns executionStepInfo
        every { executionStepInfo.getArgument<String>("ident") } returns "Z990077"
        every { persondataFasade.hentFamiliemedlemmerFraIdent("Z990077") } returns medlemmer


        val familieDtoListe = familiemedlemmerDataFetcher.get(dataFetchingEnvironment)


        assertFetched(familieDtoListe)
    }

    private fun assertFetched(familieDtoListe: List<FamiliemedlemDto>) {
        familieDtoListe shouldContainExactlyInAnyOrder listOf(
            FamiliemedlemDto("etternavn barn", "fnrBarn", Familierelasjon.BARN, 42, "felles", "fnrAnnenForelder", null),
            FamiliemedlemDto("etternavn fornavn", "fnr", Familierelasjon.RELATERT_VED_SIVILSTAND, null, null, null, lagSivilstandGift())
        )
    }

    private fun lagBarn() = Familiemedlem(
        Folkeregisteridentifikator("fnrBarn"),
        Navn("barn", null, "etternavn"),
        Familierelasjon.BARN,
        Foedsel(LocalDate.now().minusYears(42), null, null, null),
        Folkeregisteridentifikator("fnrAnnenForelder"),
        "felles",
        null
    )

    private fun lagRelatertVedsivilstand() = Familiemedlem(
        Folkeregisteridentifikator("fnr"),
        Navn("fornavn", null, "etternavn"),
        Familierelasjon.RELATERT_VED_SIVILSTAND,
        Foedsel(LocalDate.MIN, null, null, null),
        null,
        "ukjent",
        lagSivilstandGift()
    )

    private fun lagSivilstandGift() = Sivilstand(
        Sivilstandstype.GIFT,
        null,
        "relatertVedSivilstandID",
        LocalDate.MIN,
        null,
        "Dolly",
        "PDL",
        false
    )
}