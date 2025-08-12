package no.nav.melosys.saksflyt.steg.satsendring

import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.prosessinstansForTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TibakestillTrygdeavgiftTest {
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var tibakestillTrygdeavgift: TibakestillTrygdeavgift

    @BeforeEach
    fun setUp() {
        tibakestillTrygdeavgift = TibakestillTrygdeavgift(behandlingsresultatService)
    }

    @Test
    fun `skal tilbakestille trygdeavgift når relevant aktiv behandling finnes`() {
        val prosessinstans = prosessinstansForTest {
            behandling {
                id = 1L
                fagsak = Fagsak.forTest()
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = Behandling.forTest {
                id = 1L
                fagsak = Fagsak.forTest()
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
            medlemskapsperioder = lagMedlemskapsperioder(this)
        }
        every {
            behandlingsresultatService.hentBehandlingsresultat(Behandling.forTest {
                id = 1L
                fagsak = Fagsak.forTest()
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.UNDER_BEHANDLING
            }.id)
        } returns behandlingsresultat


        tibakestillTrygdeavgift.utfør(prosessinstans)


        behandlingsresultat.trygdeavgiftsperioder.shouldBeEmpty()
    }

    private fun lagMedlemskapsperioder(behandlingsresultat: Behandlingsresultat): List<Medlemskapsperiode> {
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.EPOCH.plusMonths(1)
            tom = LocalDate.EPOCH.plusMonths(4)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            this.behandlingsresultat = behandlingsresultat
        }
        medlemskapsperiode.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()

        return listOf(medlemskapsperiode)
    }

    private fun lagTrygdeavgiftsperioder(): Set<Trygdeavgiftsperiode> {
        val trygdeavgift = Trygdeavgiftsperiode(
            periodeFra = LocalDate.now(),
            periodeTil = LocalDate.now().plusDays(10),
            trygdesats = BigDecimal.valueOf(7.9),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(10000.0)),

            grunnlagMedlemskapsperiode = Medlemskapsperiode().apply {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            },

            grunnlagInntekstperiode = Inntektsperiode().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigTotalinntekt = Penger(5000.0)
            },

            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        return mutableSetOf(trygdeavgift)
    }
}
