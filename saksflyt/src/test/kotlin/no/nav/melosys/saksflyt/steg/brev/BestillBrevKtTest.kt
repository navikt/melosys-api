package no.nav.melosys.saksflyt.steg.brev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BestillBrevKtTest {

    @Mock
    private lateinit var brevBestiller: BrevBestiller

    private lateinit var bestillBrev: BestillBrev

    @BeforeEach
    fun setUp() {
        bestillBrev = BestillBrev(brevBestiller)
    }

    @Test
    fun utfør_altOk_kallerBestill() {
        val behandling = BehandlingTestFactory.builderWithDefaults().build()
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        val brevbestilling = DoksysBrevbestilling.Builder()
            .medProduserbartDokument(INNVILGELSE_YRKESAKTIV)
            .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER))
            .build()
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling)
        val captor = ArgumentCaptor.forClass(DoksysBrevbestilling::class.java)

        bestillBrev.utfør(prosessinstans)

        verify(brevBestiller).bestill(captor.capture())
        val bestiltBrevbestilling = captor.value
        brevbestilling.behandling shouldBe null
        bestiltBrevbestilling.behandling shouldBe behandling
        bestiltBrevbestilling.produserbartdokument shouldBe brevbestilling.produserbartdokument
        bestiltBrevbestilling.mottakere shouldBe brevbestilling.mottakere
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
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = BehandlingTestFactory.builderWithDefaults().build()

        val exception = shouldThrow<FunksjonellException> {
            bestillBrev.utfør(prosessinstans)
        }
        exception.message shouldContain "Prosessinstans mangler brevbestilling"
    }

    @Test
    fun utfør_flereEnnEnMottaker_kasterFeilmelding() {
        val behandling = BehandlingTestFactory.builderWithDefaults().build()
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        prosessinstans.setData(
            ProsessDataKey.BREVBESTILLING,
            DoksysBrevbestilling.Builder()
                .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER))
                .build()
        )

        val exception = shouldThrow<FunksjonellException> {
            bestillBrev.utfør(prosessinstans)
        }
        exception.message shouldContain "Prosessinstans skal sende brev til én mottaker, fant 2"
    }
}
