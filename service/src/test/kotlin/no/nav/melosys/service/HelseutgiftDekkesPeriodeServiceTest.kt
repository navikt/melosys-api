package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.HelseutgiftDekkesPeriodeRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class HelseutgiftDekkesPeriodeServiceTest {

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeRepository: HelseutgiftDekkesPeriodeRepository

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    fun beforeEach() {
        helseutgiftDekkesPeriodeService = HelseutgiftDekkesPeriodeService(helseutgiftDekkesPeriodeRepository, behandlingsresultatService)

        behandlingsresultat = Behandlingsresultat().apply {
            id = BEH_ID
        }
    }

    @Test
    fun finnHelseutgiftDekkesPeriode_ingenPeriode_kasterException() {
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns null

        helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(BEH_ID).run {
            this shouldBe null
        }
    }

    @Test
    fun finnHelseutgiftDekkesPeriode_periodeEksisterer_girResultat() {
        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode()
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns helseutgiftDekkesPeriode

        helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(BEH_ID)!!.run {
            this.behandlingsresultat shouldBe helseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe helseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe helseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe helseutgiftDekkesPeriode.bostedLandkode
        }
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode() {
        val lagretBehandlingsresultat = Behandlingsresultat().apply { id = BEH_ID }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns lagretBehandlingsresultat
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, BOSTEDLANDKODE).run {
            this.behandlingsresultat shouldBe lagretBehandlingsresultat
            this.fomDato shouldBe FOM_DATO
            this.tomDato shouldBe TOM_DATO
            this.bostedLandkode shouldBe BOSTEDLANDKODE
        }
    }

    @Test
    fun `Lagret trygdeavgift skal fjernes og helseutgift dekkes periode skal oppdateres med data`() {
        val lagretHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode().apply {
            trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
                    id = 1L,
                    periodeFra = this.fomDato,
                    periodeTil = this.tomDato,
                    trygdeavgiftsbeløpMd = Penger(790.0),
                    trygdesats = BigDecimal.valueOf(7.9)
                )
            )
        }

        val oppdatertHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(bostedsland = Land_iso2.NO)

        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns lagretHelseutgiftDekkesPeriode
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO).run {
            this.behandlingsresultat shouldBe oppdatertHelseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe oppdatertHelseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe oppdatertHelseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe oppdatertHelseutgiftDekkesPeriode.bostedLandkode
            this.trygdeavgiftsperioder shouldBe emptySet()

        }
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode_kasterFeil() {
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns null

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Finner ingen helseutgift-periode med behandlingID: $BEH_ID"
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode_kasterFeil() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } throws IkkeFunnetException("Finner ingen behandlingsresultat for id: $BEH_ID")

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Finner ingen behandlingsresultat for id: 1"
    }

    @Test
    fun `Annullering - slette Helseutgift Dekkes Periode`() {
        val lagretHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode().apply {
            id = 777L
            trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
                    id = 1L,
                    periodeFra = this.fomDato,
                    periodeTil = this.tomDato,
                    trygdeavgiftsbeløpMd = Penger(790.0),
                    trygdesats = BigDecimal.valueOf(7.9)
                )
            )
        }

        behandlingsresultat.apply {
            helseutgiftDekkesPeriode = lagretHelseutgiftDekkesPeriode
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns lagretHelseutgiftDekkesPeriode


        helseutgiftDekkesPeriodeService.slettHelseutgiftDekkesPeriode(BEH_ID)


        behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.shouldBeEmpty()
        behandlingsresultat.helseutgiftDekkesPeriode.shouldBe(null)
    }

    private fun lagHelseutgiftDekkesPeriode(bostedsland: Land_iso2 = BOSTEDLANDKODE): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = FOM_DATO,
            tomDato = TOM_DATO,
            bostedLandkode = bostedsland
        )
    }

    companion object {
        private val BEH_ID = 1L
        private val FOM_DATO = LocalDate.of(2025, 1, 1)
        private val TOM_DATO = LocalDate.of(2025, 1, 2)
        private val BOSTEDLANDKODE = Land_iso2.BA
    }
}
