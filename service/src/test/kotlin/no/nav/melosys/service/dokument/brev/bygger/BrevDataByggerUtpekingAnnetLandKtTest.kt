package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.utpeking.UtpekingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BrevDataByggerUtpekingAnnetLandKtTest {
    @MockK
    private lateinit var utpekingService: UtpekingService

    @MockK
    private lateinit var brevDataGrunnlag: BrevDataGrunnlag

    private lateinit var brevDataByggerUtpekingAnnetLand: BrevDataByggerUtpekingAnnetLand

    @BeforeEach
    fun setUp() {
        val behandling = Behandling.forTest {
            id = 1L
        }
        every { brevDataGrunnlag.behandling } returns behandling
        brevDataByggerUtpekingAnnetLand = BrevDataByggerUtpekingAnnetLand(utpekingService, BrevbestillingDto())
    }

    @Test
    fun `skal bygge utpekingsbrev med gyldig utpekingsperiode`() {
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), null, Land_iso2.CY,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        )
        every { utpekingService.hentUtpekingsperioder(1L) } returns listOf(utpekingsperiode)


        val brevData = brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb")


        brevData.run {
            shouldBeInstanceOf<BrevDataUtpekingAnnetLand>()
            utpekingsperiode shouldBe utpekingsperiode
        }
    }

    @Test
    fun `skal kaste exception når ingen utpekingsperioder finnes`() {
        every { utpekingService.hentUtpekingsperioder(1L) } returns emptyList()


        val exception = shouldThrow<FunksjonellException> {
            brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb")
        }


        exception.message shouldContain "uten utpekingsperiode"
    }
}
