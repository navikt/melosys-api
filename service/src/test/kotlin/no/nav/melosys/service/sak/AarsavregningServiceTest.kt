package no.nav.melosys.service.sak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class AarsavregningServiceTest {
    @RelaxedMockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private var fagsak = Fagsak().apply {
        saksnummer = "12345"
        type = Sakstyper.FTRL
        tema = Sakstemaer.TRYGDEAVGIFT
        status = Saksstatuser.OPPRETTET
        behandlinger.add(Behandling().apply {
            id = 12345
        })
    }

    private var behandlingsresultat = Behandlingsresultat()

    private lateinit var aarsavregningService: AarsavregningService

    @BeforeEach
    fun setup() {
        aarsavregningService = AarsavregningService(fagsakService, behandlingsresultatService)
    }

    @Test
    fun `sjekk hentEksisterendeTrygdeavgiftsperioderForFagsak kun returnerer trygdeavgiftsperioder for forespurt år`() {
        val saksnummer = "12345"
        val year = 2023

        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(1000.0)
            trygdesats = BigDecimal.ZERO
            periodeFra = LocalDate.of(2021, 1, 11)
            periodeTil = LocalDate.of(2021, 10, 11)
        }
        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(2345.56)
            trygdesats = BigDecimal(3.56)
            periodeFra = LocalDate.of(2023, 1, 11)
            periodeTil = LocalDate.of(2023, 10, 11)
        }
        behandlingsresultat.apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode1)
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode2)
                }
                fakturaserieReferanse = "FakturaserieReferanse"
            }
        }

        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        aarsavregningService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, year).shouldBe(setOf(trygdeavgiftsperiode2))
    }
}
