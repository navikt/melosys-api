package no.nav.melosys.domain

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.ManglendeInnbetalingHandlingsvalg
import kotlin.test.Test

class BehandlingsresultatHarValgtFullstendigManglendeInnbetalingTest {

    @Test
    fun `returnerer true når kun eldre boolsk fakta er satt til VALGT_FAKTA`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            avklartefakta {
                type = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
                referanse = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode
                fakta = Avklartefakta.VALGT_FAKTA
            }
        }

        behandlingsresultat.harValgtFullstendigManglendeInnbetaling().shouldBeTrue()
    }

    @Test
    fun `returnerer true når kun nytt enum-fakta er satt til HELE_PERIODEN_OPPHØRES`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            avklartefakta {
                type = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG
                referanse = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG.kode
                fakta = ManglendeInnbetalingHandlingsvalg.HELE_PERIODEN_OPPHØRES.kode
            }
        }

        behandlingsresultat.harValgtFullstendigManglendeInnbetaling().shouldBeTrue()
    }

    @Test
    fun `nytt enum-fakta vinner over eldre boolsk fakta når begge finnes og er motstridende`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            avklartefakta {
                type = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
                referanse = Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode
                fakta = Avklartefakta.VALGT_FAKTA
            }
            avklartefakta {
                type = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG
                referanse = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG.kode
                fakta = ManglendeInnbetalingHandlingsvalg.DELER_AV_PERIODEN_OPPHØRES.kode
            }
        }

        behandlingsresultat.harValgtFullstendigManglendeInnbetaling().shouldBeFalse()
    }

    @Test
    fun `returnerer false når ingen av faktaene er satt`() {
        val behandlingsresultat = Behandlingsresultat.forTest {}

        behandlingsresultat.harValgtFullstendigManglendeInnbetaling().shouldBeFalse()
    }
}
