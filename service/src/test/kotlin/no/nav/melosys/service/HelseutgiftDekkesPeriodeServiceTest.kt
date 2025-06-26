package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.HelseutgiftDekkesPeriodeRepository
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*


@ExtendWith(MockKExtension::class)
internal class HelseutgiftDekkesPeriodeServiceTest {

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeRepository: HelseutgiftDekkesPeriodeRepository

    @RelaxedMockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @InjectMockKs
    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    companion object {
        private val BEH_ID = 1L
        private val FOM_DATO = LocalDate.of(2025, 1, 1)
        private val TOM_DATO = LocalDate.of(2025, 1, 2)
        private val BOSTEDLANDKODE = Land_iso2.BA
    }

    @Test
    fun hentHelseutgiftDekkesPeriode_ingenPeriode_kasterException() {
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns null


        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(BEH_ID)
        }.message shouldBe "Finner ingen helseutgift-periode med behandlingID: $BEH_ID"
    }

    @Test
    fun hentHelseutgiftDekkesPeriode_periodeEksisterer_girResultat() {
        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode()
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns helseutgiftDekkesPeriode

        helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(BEH_ID).run {
            this.behandlingsresultat shouldBe helseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe helseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe helseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe helseutgiftDekkesPeriode.bostedLandkode
        }
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode() {
        val lagretBehandlingsresultat = Behandlingsresultat().apply { id = BEH_ID }
        every { behandlingsresultatRepository.findById(BEH_ID) } returns Optional.of(lagretBehandlingsresultat)
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, BOSTEDLANDKODE).run {
            this.behandlingsresultat shouldBe lagretBehandlingsresultat
            this.fomDato shouldBe FOM_DATO
            this.tomDato shouldBe TOM_DATO
            this.bostedLandkode shouldBe BOSTEDLANDKODE
        }
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode() {
        val lagretHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode()
        val oppdatertHelseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(bostedsland = Land_iso2.NO)

        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns lagretHelseutgiftDekkesPeriode
        every { helseutgiftDekkesPeriodeRepository.save(any()) } answers { firstArg() }

        helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO).run {
            this.behandlingsresultat shouldBe oppdatertHelseutgiftDekkesPeriode.behandlingsresultat
            this.fomDato shouldBe oppdatertHelseutgiftDekkesPeriode.fomDato
            this.tomDato shouldBe oppdatertHelseutgiftDekkesPeriode.tomDato
            this.bostedLandkode shouldBe oppdatertHelseutgiftDekkesPeriode.bostedLandkode
        }
    }

    @Test
    fun oppdaterHelseutgiftDekkesPeriode_kasterFeil() {
        every { helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(BEH_ID) } returns null

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Finner ingen helseutgift-periode med behandlingID: 1"
    }

    @Test
    fun opprettHelseutgiftDekkesPeriode_kasterFeil() {
        every { behandlingsresultatRepository.findById(BEH_ID) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(BEH_ID, FOM_DATO, TOM_DATO, Land_iso2.NO)
        }.message shouldBe "Finner ingen behandlingsresultat for id: 1"
    }

    fun lagHelseutgiftDekkesPeriode(bostedsland: Land_iso2 = BOSTEDLANDKODE): HelseutgiftDekkesPeriode {
        return HelseutgiftDekkesPeriode(
            behandlingsresultat = Behandlingsresultat(),
            fomDato = FOM_DATO,
            tomDato = TOM_DATO,
            bostedLandkode = bostedsland
        )
    }
}
