package no.nav.melosys.saksflyt.steg.sed

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettAnmodningsperiodeSvarTest {

    @MockK(relaxUnitFun = true)
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    private lateinit var opprettAnmodningsperiodeSvar: OpprettAnmodningsperiodeSvar

    @BeforeEach
    fun setup() {
        opprettAnmodningsperiodeSvar = OpprettAnmodningsperiodeSvar(anmodningsperiodeService)
    }

    @Test
    fun `utfør med mottatt innvilgelse skal returnere svar med type innvilgelse`() {
        val prosessinstans = hentProsessinstans(true)


        opprettAnmodningsperiodeSvar.utfør(prosessinstans)


        val captor = slot<AnmodningsperiodeSvar>()
        verify { anmodningsperiodeService.lagreAnmodningsperiodeSvarForBehandling(any(), capture(captor)) }

        captor.captured.run {
            anmodningsperiodeSvarType shouldBe Anmodningsperiodesvartyper.INNVILGELSE
            anmodningsperiode.shouldBeNull()
        }
    }

    @Test
    fun `utfør med mottatt delvis innvilgelse skal returnere svar med type innvilgelse`() {
        val prosessinstans = hentProsessinstans(false)


        opprettAnmodningsperiodeSvar.utfør(prosessinstans)


        val captor = slot<AnmodningsperiodeSvar>()
        verify { anmodningsperiodeService.lagreAnmodningsperiodeSvarForBehandling(any(), capture(captor)) }

        captor.captured.run {
            anmodningsperiodeSvarType shouldBe Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            begrunnelseFritekst.shouldNotBeNull()
            innvilgetFom.shouldNotBeNull()
            innvilgetTom.shouldNotBeNull()
        }
    }

    private fun hentProsessinstans(innvilgelse: Boolean) = Prosessinstans.forTest {
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
        medData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(innvilgelse))
        behandling {
            id = 123L
        }
    }

    private fun hentMelosysEessiMelding(innvilgelse: Boolean) = MelosysEessiMelding().apply {
        gsakSaksnummer = 123L
        svarAnmodningUnntak = if (innvilgelse) {
            SvarAnmodningUnntak(
                begrunnelse = "blabla fritekst",
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE
            )
        } else {
            SvarAnmodningUnntak(
                begrunnelse = "blabla fritekst",
                beslutning = SvarAnmodningUnntak.Beslutning.DELVIS_INNVILGELSE,
                delvisInnvilgetPeriode = Periode(
                    fom = LocalDate.of(2012, 12, 12),
                    tom = LocalDate.of(2012, 12, 12)
                )
            )
        }
    }
}
