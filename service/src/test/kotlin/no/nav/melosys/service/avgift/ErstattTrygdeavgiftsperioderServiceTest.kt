package no.nav.melosys.service.avgift

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ErstattTrygdeavgiftsperioderServiceTest() {
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @InjectMockKs
    private lateinit var erstattTrygdeavgiftsperioderService: ErstattTrygdeavgiftsperioderService

    @Nested
    inner class ErstattTrygdeavgiftsperioderTest {
        @Test
        fun `erstatter eksisterende Trygdeavgiftsperioder`() {
            val eksisterendeId = 1L
            val nyId = 2L
            val medlId = 3L

            val eksisterendeTrygdeavgiftsperiodeMock = mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { id } returns eksisterendeId
                every { grunnlagMedlemskapsperiode?.id } returns medlId
            }
            val nyTrygdeavgiftsperiodeMock = mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { id } returns nyId
                every { grunnlagMedlemskapsperiode?.id } returns medlId
            }

            val medlemskap = Medlemskapsperiode().apply { this.id = medlId }
            medlemskap.addTrygdeavgiftsperiode(eksisterendeTrygdeavgiftsperiodeMock)

            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling()
                medlemskapsperioder = listOf(medlemskap)
            }

            every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

            val nyeTrygdeavgiftsperioder = listOf(nyTrygdeavgiftsperiodeMock)


            erstattTrygdeavgiftsperioderService.erstattTrygdeavgiftsperioder(1337L, nyeTrygdeavgiftsperioder)


            behandlingsresultat.trygdeavgiftType shouldBeEqual Trygdeavgift_typer.FORELØPIG
            medlemskap.trygdeavgiftsperioder shouldBe nyeTrygdeavgiftsperioder
        }

        @Test
        fun erPliktigMedlemskapSkattepliktig() {
        }

        @Test
        fun erstattTrygdeavgiftsperioder() {
        }

        @Test
        fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig() {
        }

    }
}
