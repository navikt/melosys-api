package no.nav.melosys.saksflyt.steg.satsendring

import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling as prosessinstansBehandling
import no.nav.melosys.saksflytapi.domain.forTest
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
        val behandlingId = 1L

        val prosessinstans = Prosessinstans.forTest {
            prosessinstansBehandling {
                id = behandlingId
                fagsak { }
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = behandlingId
                fagsak { }
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
            medlemskapsperiode {
                fom = LocalDate.EPOCH.plusMonths(1)
                tom = LocalDate.EPOCH.plusMonths(4)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now()
                    periodeTil = LocalDate.now().plusDays(10)
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = BigDecimal.valueOf(10000.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigTotalinntekt = Penger(5000.0)
                    }
                    grunnlagSkatteforholdTilNorge {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat


        tibakestillTrygdeavgift.utfør(prosessinstans)


        behandlingsresultat.trygdeavgiftsperioder.shouldBeEmpty()
    }
}
