package no.nav.melosys.saksflyt.steg.brev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BestillBrevTest {

    private val brevBestiller: BrevBestiller = mockk()

    private lateinit var bestillBrev: BestillBrev

    @BeforeEach
    fun setUp() {
        bestillBrev = BestillBrev(brevBestiller)
    }

    @Test
    fun utfør_altOk_kallerBestill() {
        val behandling = Behandling.forTest { }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
        }
        val brevbestilling = DoksysBrevbestilling.Builder()
            .medProduserbartDokument(INNVILGELSE_YRKESAKTIV)
            .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER))
            .build()
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling)
        val slot = slot<DoksysBrevbestilling>()
        every { brevBestiller.bestill(capture(slot)) } returns Unit


        bestillBrev.utfør(prosessinstans)


        verify { brevBestiller.bestill(any()) }
        val bestiltBrevbestilling = slot.captured
        brevbestilling.behandling shouldBe null
        bestiltBrevbestilling.run {
            this.behandling shouldBe behandling
            produserbartdokument shouldBe brevbestilling.produserbartdokument
            mottakere shouldBe brevbestilling.mottakere
        }
    }

    @Test
    fun utfør_manglerBehandling_kasterFeilmelding() {
        val prosessinstans = Prosessinstans()


        val exception = shouldThrow<FunksjonellException> {
            bestillBrev.utfør(prosessinstans)
        }


        exception.message shouldContain "Prosessinstans mangler behandling"
    }

    @Test
    fun utfør_manglerBrevbestilling_kasterFeilmelding() {
        val prosessinstans = Prosessinstans.forTest {
            behandling = Behandling.forTest { }
        }


        val exception = shouldThrow<FunksjonellException> {
            bestillBrev.utfør(prosessinstans)
        }


        exception.message shouldContain "Prosessinstans mangler brevbestilling"
    }

    @Test
    fun utfør_flereEnnEnMottaker_kasterFeilmelding() {
        val behandling = Behandling.forTest { }
        val prosessinstans = Prosessinstans.forTest {
            this.behandling = behandling
            medData(
                ProsessDataKey.BREVBESTILLING,
                DoksysBrevbestilling.Builder()
                    .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER))
                    .build()
            )
        }


        val exception = shouldThrow<FunksjonellException> {
            bestillBrev.utfør(prosessinstans)
        }


        exception.message shouldContain "Prosessinstans skal sende brev til én mottaker, fant 2"
    }
}
