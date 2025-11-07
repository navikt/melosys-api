package no.nav.melosys.service.avgift

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TrygdeavgiftperiodeErstatterTest() {
    @MockK(relaxed = true)
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    private lateinit var trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter

    @BeforeEach
    fun beforeEach() {
        trygdeavgiftperiodeErstatter = TrygdeavgiftperiodeErstatter(behandlingsresultatService)
    }

    @Test
    fun `erstatter eksisterende Trygdeavgiftsperioder`() {
        val medlId = 3L

        val eksisterendeTrygdeavgiftsperiode = lagTrygdeavgiftsperioder(medlId, 1L).single()
        val nyTrygdeavgiftsperiode = lagTrygdeavgiftsperioder(medlId, 2L).single()

        val medlemskap = lagMedlemskap(medlId, listOf(eksisterendeTrygdeavgiftsperiode))
        val behandlingsresultat = lagBehandlingsresultat(medlemskap)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val nyeTrygdeavgiftsperioder = listOf(nyTrygdeavgiftsperiode)

        // Act
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

        // Assert
        behandlingsresultat.trygdeavgiftType.shouldNotBeNull() shouldBeEqual Trygdeavgift_typer.FORELØPIG
        medlemskap.trygdeavgiftsperioder shouldContainExactly nyeTrygdeavgiftsperioder.toSet()
    }


    @Test
    fun `erstatter eksisterende Trygdeavgiftsperioder for lovvalgsperioder`() {
        val medlId = 3L

        val eksisterendeTrygdeavgiftsperiode = lagTrygdeavgiftsperioderForLovvalg(medlId, 1L).single()
        val nyTrygdeavgiftsperiode = lagTrygdeavgiftsperioderForLovvalg(medlId, 2L).single()

        val lovvalgsperiode = lagLovvalgsperiode(medlId, listOf(eksisterendeTrygdeavgiftsperiode))
        val behandlingsresultat = lagBehandlingsresultatMedLovvalgsperioder(lovvalgsperiode)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val nyeTrygdeavgiftsperioder = listOf(nyTrygdeavgiftsperiode)

        // Act
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

        // Assert
        behandlingsresultat.trygdeavgiftType.shouldNotBeNull() shouldBeEqual Trygdeavgift_typer.FORELØPIG
        lovvalgsperiode.trygdeavgiftsperioder shouldContainExactly nyeTrygdeavgiftsperioder.toSet()
    }

    @Test
    fun `erstatter eksisterende Trygdeavgiftsperioder for EØS pensjonist`() {
        val helseutgiftDekkesPeriodeId = 3L
        val FOM = LocalDate.now()
        val TOM = LocalDate.now().plusMonths(2)
        val bostedLand = Land_iso2.DK

        val eksisterendeTrygdeavgiftsperiode = lagTrygdeavgiftsperioder(helseutgiftDekkesPeriodeId, 1L, erEøsPensjonist = true).single()
        val nyTrygdeavgiftsperiode = lagTrygdeavgiftsperioder(helseutgiftDekkesPeriodeId, 2L, erEøsPensjonist = true).single()

        val behandlingsresultat = lagBehandlingsresultat()
        val helseutgiftDekkesPeriode =
            lagHelseutgiftDekkesPeriode(
                behandlingsresultat,
                helseutgiftDekkesPeriodeId,
                listOf(eksisterendeTrygdeavgiftsperiode),
                FOM,
                TOM,
                bostedLand
            )
        behandlingsresultat.apply {
            this.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val nyeTrygdeavgiftsperioder = listOf(nyTrygdeavgiftsperiode)

        // Act
        trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

        // Assert
        nyeTrygdeavgiftsperioder.forEach { trygdeavgiftsperiode ->
            trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode?.id?.shouldBeEqual(helseutgiftDekkesPeriodeId)
        }
        behandlingsresultat.trygdeavgiftType.shouldNotBeNull() shouldBeEqual Trygdeavgift_typer.FORELØPIG
        helseutgiftDekkesPeriode.trygdeavgiftsperioder shouldContainExactly nyeTrygdeavgiftsperioder.toSet()
    }

    @Test
    fun `erstatter flere eksisterende Trygdeavgiftsperioder for flere medlemskap`() {
        val medlId1 = 1L
        val medlId2 = 2L

        val eksisterendeTrygdeavgiftsperioder1 = lagTrygdeavgiftsperioder(medlId1, 101L, 102L)
        val eksisterendeTrygdeavgiftsperioder2 = lagTrygdeavgiftsperioder(medlId2, 103L, 104L)

        val nyTrygdeavgiftsperioder1 = lagTrygdeavgiftsperioder(medlId1, 201L, 202L)
        val nyTrygdeavgiftsperioder2 = lagTrygdeavgiftsperioder(medlId2, 203L, 204L)

        val medlemskap1 = lagMedlemskap(medlId1, eksisterendeTrygdeavgiftsperioder1)
        val medlemskap2 = lagMedlemskap(medlId2, eksisterendeTrygdeavgiftsperioder2)

        val behandlingsresultat = lagBehandlingsresultat(medlemskap1, medlemskap2)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val nyeTrygdeavgiftsperioder = nyTrygdeavgiftsperioder1 + nyTrygdeavgiftsperioder2

        // Act
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

        // Assert
        medlemskap1.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder1.toSet()
        medlemskap2.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder2.toSet()
        behandlingsresultat.trygdeavgiftType shouldBe Trygdeavgift_typer.FORELØPIG
    }

    @Test
    fun `erstatter flere eksisterende Trygdeavgiftsperioder for flere lovvalgsperioder`() {
        val medlId1 = 1L
        val medlId2 = 2L

        val eksisterendeTrygdeavgiftsperioder1 = lagTrygdeavgiftsperioderForLovvalg(medlId1, 101L, 102L)
        val eksisterendeTrygdeavgiftsperioder2 = lagTrygdeavgiftsperioderForLovvalg(medlId2, 103L, 104L)

        val nyTrygdeavgiftsperioder1 = lagTrygdeavgiftsperioderForLovvalg(medlId1, 201L, 202L)
        val nyTrygdeavgiftsperioder2 = lagTrygdeavgiftsperioderForLovvalg(medlId2, 203L, 204L)

        val medlemskap1 = lagLovvalgsperiode(medlId1, eksisterendeTrygdeavgiftsperioder1)
        val medlemskap2 = lagLovvalgsperiode(medlId2, eksisterendeTrygdeavgiftsperioder2)

        val behandlingsresultat = lagBehandlingsresultatMedLovvalgsperioder(medlemskap1, medlemskap2)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        val nyeTrygdeavgiftsperioder = nyTrygdeavgiftsperioder1 + nyTrygdeavgiftsperioder2

        // Act
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)

        // Assert
        medlemskap1.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder1.toSet()
        medlemskap2.trygdeavgiftsperioder shouldContainExactly nyTrygdeavgiftsperioder2.toSet()
        behandlingsresultat.trygdeavgiftType shouldBe Trygdeavgift_typer.FORELØPIG
    }


    private fun lagTrygdeavgiftsperioder(grunnlagPeriodeId: Long, vararg ids: Long, erEøsPensjonist: Boolean = false): List<Trygdeavgiftsperiode> {
        if (erEøsPensjonist) {
            return ids.map { periodeId ->
                mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                    every { id } returns periodeId
                    every { grunnlagHelseutgiftDekkesPeriode?.id } returns grunnlagPeriodeId
                }
            }
        }
        return ids.map { periodeId ->
            mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { id } returns periodeId
                every { grunnlagMedlemskapsperiode?.id } returns grunnlagPeriodeId
            }
        }
    }

    private fun lagTrygdeavgiftsperioderForLovvalg(grunnlagPeriodeId: Long, vararg ids: Long): List<Trygdeavgiftsperiode> {
        return ids.map { periodeId ->
            mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { id } returns periodeId
                every { grunnlagLovvalgsPeriode?.id } returns grunnlagPeriodeId
            }
        }
    }

    private fun lagMedlemskap(
        medlemskapId: Long,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            id = medlemskapId
            trygdeavgiftsperioder.forEach { addTrygdeavgiftsperiode(it) }
        }
    }

    private fun lagLovvalgsperiode(
        lovvalgsperiodeId: Long,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): Lovvalgsperiode {
        return Lovvalgsperiode().apply {
            id = lovvalgsperiodeId
            trygdeavgiftsperioder.forEach { addTrygdeavgiftsperiode(it) }
        }
    }


    private fun lagHelseutgiftDekkesPeriode(
        behandlingsresultat: Behandlingsresultat,
        helseutgiftDekkesPeriodeId: Long,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>,
        fomDato: LocalDate,
        tomDato: LocalDate,
        landKode: Land_iso2
    ): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = fomDato,
            tomDato = tomDato,
            bostedLandkode = landKode
        ).apply {
            id = helseutgiftDekkesPeriodeId
            trygdeavgiftsperioder.forEach { this.trygdeavgiftsperioder.add(it) }
        }
    }

    private fun lagBehandlingsresultat(
        vararg medlemskapsperioder: Medlemskapsperiode? = emptyArray(),
        helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null
    ): Behandlingsresultat {
        helseutgiftDekkesPeriode?.let {
            return Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    fagsak?.type = Sakstyper.FTRL
                }
                this.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode
            }
        }

        return Behandlingsresultat().apply {
            behandling = Behandling.forTest {
                fagsak?.type = Sakstyper.FTRL
            }
            this.medlemskapsperioder = medlemskapsperioder.filterNotNull().toMutableSet()
        }
    }


    private fun lagBehandlingsresultatMedLovvalgsperioder(
        vararg lovvalgsperioder: Lovvalgsperiode? = emptyArray(),
    ): Behandlingsresultat {

        return Behandlingsresultat().apply {
            behandling = Behandling.forTest {
                fagsak?.type = Sakstyper.EU_EOS
                fagsak?.tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            this.lovvalgsperioder = lovvalgsperioder.filterNotNull().toMutableSet()
        }
    }
}
