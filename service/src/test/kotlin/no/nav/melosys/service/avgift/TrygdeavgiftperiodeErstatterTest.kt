package no.nav.melosys.service.avgift

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TrygdeavgiftperiodeErstatterTest() {
    @MockK(relaxed = true)
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    private lateinit var trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter

    @BeforeEach
    fun beforeEach() {
        trygdeavgiftperiodeErstatter = TrygdeavgiftperiodeErstatter(behandlingsresultatService)
    }

    @Nested
    inner class ErstattTrygdeavgiftsperioderTest {
        @Test
        fun `erstatter eksisterende Trygdeavgiftsperioder`() {
            val medlId = 3L

            val eksisterendeTrygdeavgiftsperiode = createTrygdeavgiftsperioder(medlId, 1L).single()
            val nyTrygdeavgiftsperiode = createTrygdeavgiftsperioder(medlId, 2L).single()

            val medlemskap = createMedlemskap(medlId, listOf(eksisterendeTrygdeavgiftsperiode))
            val behandlingsresultat = createBehandlingsresultat(medlemskap)

            every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

            val nyeTrygdeavgiftsperioder = listOf(nyTrygdeavgiftsperiode)

            // Act
            trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

            // Assert
            behandlingsresultat.trygdeavgiftType shouldBeEqual Trygdeavgift_typer.FORELØPIG
            medlemskap.trygdeavgiftsperioder shouldContainExactly nyeTrygdeavgiftsperioder.toSet()
        }

        @Test
        fun `erstatter flere eksisterende Trygdeavgiftsperioder for flere medlemskap`() {
            val medlId1 = 1L
            val medlId2 = 2L

            val eksisterendeTrygdeavgiftsperioder1 = createTrygdeavgiftsperioder(medlId1, 101L, 102L)
            val eksisterendeTrygdeavgiftsperioder2 = createTrygdeavgiftsperioder(medlId2, 103L, 104L)

            val nyTrygdeavgiftsperioder1 = createTrygdeavgiftsperioder(medlId1, 201L, 202L)
            val nyTrygdeavgiftsperioder2 = createTrygdeavgiftsperioder(medlId2, 203L, 204L)

            val medlemskap1 = createMedlemskap(medlId1, eksisterendeTrygdeavgiftsperioder1)
            val medlemskap2 = createMedlemskap(medlId2, eksisterendeTrygdeavgiftsperioder2)

            val behandlingsresultat = createBehandlingsresultat(medlemskap1, medlemskap2)

            every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

            val nyeTrygdeavgiftsperioder = nyTrygdeavgiftsperioder1 + nyTrygdeavgiftsperioder2

            // Act
            trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

            // Assert
            medlemskap1.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder1.toSet()
            medlemskap2.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder2.toSet()
            behandlingsresultat.trygdeavgiftType shouldBe Trygdeavgift_typer.FORELØPIG
        }
    }

    @Nested
    inner class LeggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktigTest {
        @Test
        fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig() {
        }
    }

    private fun createTrygdeavgiftsperioder(medlemskapId: Long, vararg ids: Long): List<Trygdeavgiftsperiode> {
        return ids.map { periodeId ->
            mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { id } returns periodeId
                every { grunnlagMedlemskapsperiode?.id } returns medlemskapId
            }
        }
    }

    private fun createMedlemskap(
        medlemskapId: Long,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            id = medlemskapId
            trygdeavgiftsperioder.forEach { addTrygdeavgiftsperiode(it) }
        }
    }

    private fun createBehandlingsresultat(vararg medlemskapsperioder: Medlemskapsperiode): Behandlingsresultat {
        return Behandlingsresultat().apply {
            behandling = Behandling()
            this.medlemskapsperioder = medlemskapsperioder.toList()
        }
    }
}
