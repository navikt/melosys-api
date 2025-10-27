package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

internal class ÅrsavregningServiceHentSisteÅrsavregningTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `hentSisteÅrsavregning filtrerer kun FASTSATT_TRYGDEAVGIFT behandlingsresultater`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            status = Saksstatuser.OPPRETTET

            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }

            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }

            behandling {
                id = 3L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
            }
        }

        val år = 2023

        // Behandling 1: Årsavregning med FASTSATT_TRYGDEAVGIFT
        val behandlingsresultat1 = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = fagsak.behandlinger[0]
            registrertDato = LocalDate.of(2024, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2024, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                id = 10L
                aar = år
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }

        // Behandling 2: Årsavregning med FASTSATT_TRYGDEAVGIFT (skal inkluderes)
        val behandlingsresultat2 = Behandlingsresultat.forTest {
            id = 2L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = fagsak.behandlinger[1]
            registrertDato = LocalDate.of(2024, 2, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2024, 2, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                id = 20L
                aar = år
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }

        // Behandling 3: MEDLEM_I_FOLKETRYGDEN (skal filtreres bort)
        val behandlingsresultat3 = Behandlingsresultat.forTest {
            id = 3L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = fagsak.behandlinger[2]
            registrertDato = LocalDate.of(2024, 3, 5).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2024, 3, 5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            årsavregning {
                id = 30L
                aar = år
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }

        every { fagsakService.hentFagsak("123456") } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat1
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultat2
        every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultat3


        val resultat = årsavregningService.hentSisteÅrsavregning("123456", år)


        resultat.shouldNotBeNull().run {
            id shouldBe 20L
            aar shouldBe år
        }
    }
}
