package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeKilde
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
import java.util.Optional


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

        behandlingsresultat = Behandlingsresultat.forTest {
            id = BEH_ID
            behandling { id = BEH_ID }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat
    }

    @Test
    fun finnHelseutgiftDekkesPerioder_ingenPeriode_returnerTomListe() {
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatIdAndKilde(BEH_ID, HelseutgiftDekkesPeriodeKilde.MELOSYS) } returns emptyList()

        helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPerioder(BEH_ID).shouldBeEmpty()
    }

    @Test
    fun finnHelseutgiftDekkesPerioder_periodeEksisterer_girResultat() {
        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode()
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatIdAndKilde(BEH_ID, HelseutgiftDekkesPeriodeKilde.MELOSYS) } returns listOf(helseutgiftDekkesPeriode)

        helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPerioder(BEH_ID).single().run {
            this.behandlingsresultat shouldBe helseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe helseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe helseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe helseutgiftDekkesPeriode.bostedLandkode
        }
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode() {
        val lagretBehandlingsresultat = Behandlingsresultat.forTest {
            id = BEH_ID
            behandling { id = BEH_ID }
        }
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
    fun `Opprett skal opprette ny periode`() {
        val lagretBehandlingsresultat = mockk<Behandlingsresultat>(relaxed = true)

        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns lagretBehandlingsresultat
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService
            .opprettHelseutgiftDekkesPeriode(BEH_ID, NY_FOM_DATO, NY_TOM_DATO, NY_BOSTEDLANDKODE)
            .run {
                fomDato shouldBe NY_FOM_DATO
                tomDato shouldBe NY_TOM_DATO
                bostedLandkode shouldBe NY_BOSTEDLANDKODE
            }

        verify(exactly = 1) { helseutgiftDekkesPeriodeRepository.save(any()) }
    }

    @Test
    fun `Lagret trygdeavgift skal fjernes og helseutgift dekkes periode skal oppdateres med data`() {
        val lagretHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode().apply {
            id = PERIODE_ID
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

        every { helseutgiftDekkesPeriodeRepository.findById(PERIODE_ID) } returns Optional.of(lagretHelseutgiftDekkesPeriode)
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, PERIODE_ID, FOM_DATO, TOM_DATO, Land_iso2.NO).run {
            this.behandlingsresultat shouldBe oppdatertHelseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe oppdatertHelseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe oppdatertHelseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe oppdatertHelseutgiftDekkesPeriode.bostedLandkode
            this.trygdeavgiftsperioder shouldBe emptySet()

        }
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode_kasterFeil() {
        every { helseutgiftDekkesPeriodeRepository.findById(any()) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, 999L, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Finner ingen helseutgift-periode med id: 999"
    }

    @Test
    fun `oppdater skal kaste feil når periode tilhører annen behandling`() {
        val annenBehandlingsresultat = Behandlingsresultat.forTest {
            id = 999L
            behandling { id = 999L }
        }
        val periode = HelseutgiftDekkesPeriode(
            behandlingsresultat = annenBehandlingsresultat,
            fomDato = FOM_DATO,
            tomDato = TOM_DATO,
            bostedLandkode = BOSTEDLANDKODE
        ).apply { id = PERIODE_ID }

        every { helseutgiftDekkesPeriodeRepository.findById(PERIODE_ID) } returns Optional.of(periode)

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, PERIODE_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Helseutgift-periode med id $PERIODE_ID tilhører ikke behandling $BEH_ID"
    }

    @Test
    fun `oppdater skal kaste feil når periode har kilde AVGIFT_SYSTEMET`() {
        val periode = lagHelseutgiftDekkesPeriode().apply {
            id = PERIODE_ID
            kilde = HelseutgiftDekkesPeriodeKilde.AVGIFT_SYSTEMET
        }

        every { helseutgiftDekkesPeriodeRepository.findById(PERIODE_ID) } returns Optional.of(periode)

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, PERIODE_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Helseutgift-periode med id $PERIODE_ID har kilde AVGIFT_SYSTEMET og kan ikke endres"
    }

    @Test
    fun `slett skal kaste feil når periode tilhører annen behandling`() {
        val annenBehandlingsresultat = Behandlingsresultat.forTest {
            id = 999L
            behandling { id = 999L }
        }
        val periode = HelseutgiftDekkesPeriode(
            behandlingsresultat = annenBehandlingsresultat,
            fomDato = FOM_DATO,
            tomDato = TOM_DATO,
            bostedLandkode = BOSTEDLANDKODE
        ).apply { id = PERIODE_ID }

        every { helseutgiftDekkesPeriodeRepository.findById(PERIODE_ID) } returns Optional.of(periode)

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.slettHelseutgiftDekkesPeriode(BEH_ID, PERIODE_ID)
        }.message shouldBe "Helseutgift-periode med id $PERIODE_ID tilhører ikke behandling $BEH_ID"
    }

    @Test
    fun `slett skal kaste feil når periode har kilde AVGIFT_SYSTEMET`() {
        val periode = lagHelseutgiftDekkesPeriode().apply {
            id = PERIODE_ID
            kilde = HelseutgiftDekkesPeriodeKilde.AVGIFT_SYSTEMET
        }

        every { helseutgiftDekkesPeriodeRepository.findById(PERIODE_ID) } returns Optional.of(periode)

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.slettHelseutgiftDekkesPeriode(BEH_ID, PERIODE_ID)
        }.message shouldBe "Helseutgift-periode med id $PERIODE_ID har kilde AVGIFT_SYSTEMET og kan ikke endres"
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
            addHelseutgiftDekkesPeriode(lagretHelseutgiftDekkesPeriode)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat


        helseutgiftDekkesPeriodeService.slettAlleHelseutgiftDekkesPerioder(BEH_ID)


        behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.shouldBeEmpty()
        behandlingsresultat.helseutgiftDekkesPerioder.shouldBeEmpty()
    }

    @Test
    fun `Annullering - skal ikke slette når Helseutgift Dekkes Periode er null`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEH_ID) } returns behandlingsresultat


        helseutgiftDekkesPeriodeService.slettAlleHelseutgiftDekkesPerioder(BEH_ID)


        behandlingsresultat.helseutgiftDekkesPerioder.shouldBeEmpty()
        verify(exactly = 0) { behandlingsresultatService.lagreOgFlush(any())}
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
        private val PERIODE_ID = 10L
        private val FOM_DATO = LocalDate.of(2025, 1, 1)
        private val TOM_DATO = LocalDate.of(2025, 1, 2)
        private val BOSTEDLANDKODE = Land_iso2.BA
        private val NY_FOM_DATO = LocalDate.of(2025, 2, 10)
        private val NY_TOM_DATO = LocalDate.of(2025, 2, 20)
        private val NY_BOSTEDLANDKODE = Land_iso2.NO
    }
}
