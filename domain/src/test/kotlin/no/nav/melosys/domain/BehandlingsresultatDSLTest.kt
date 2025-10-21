package no.nav.melosys.domain

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import kotlin.test.Test

class BehandlingsresultatDSLTest {

    @Test
    fun `Behandlingsresultat med default verdier`() {
        val behandlingsresultat = Behandlingsresultat.forTest { }

        behandlingsresultat.run {
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            endretAv shouldBe BehandlingsresultatTestFactory.DEFAULT_ENDRET_AV
            registrertDato shouldNotBe null
            endretDato shouldNotBe null
        }
    }

    @Test
    fun `Behandlingsresultat med custom verdier`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandlingsmåte = Behandlingsmaate.AUTOMATISERT
        }

        behandlingsresultat.run {
            type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandlingsmåte shouldBe Behandlingsmaate.AUTOMATISERT
        }
    }

    @Test
    fun `Behandlingsresultat med behandling`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 123L
            }
        }

        behandlingsresultat.run {
            behandling shouldNotBe null
            behandling?.id shouldBe 123L
        }
    }

    @Test
    fun `Behandlingsresultat med vedtakMetadata`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            vedtakMetadata {
                vedtaksdato = java.time.Instant.parse("2023-01-15T10:00:00Z")
            }
        }

        behandlingsresultat.run {
            vedtakMetadata shouldNotBe null
            vedtakMetadata?.vedtaksdato shouldBe java.time.Instant.parse("2023-01-15T10:00:00Z")
            vedtakMetadata?.behandlingsresultat shouldBe behandlingsresultat
        }
    }

    @Test
    fun `Behandlingsresultat med medlemskapsperiode`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            medlemskapsperiode {
                fom = java.time.LocalDate.of(2023, 1, 1)
                tom = java.time.LocalDate.of(2023, 12, 31)
            }
        }

        behandlingsresultat.run {
            medlemskapsperioder shouldHaveSize 1
            medlemskapsperioder.first().run {
                fom shouldBe java.time.LocalDate.of(2023, 1, 1)
                tom shouldBe java.time.LocalDate.of(2023, 12, 31)
                behandlingsresultat shouldBe behandlingsresultat
            }
        }
    }

    @Test
    fun `Behandlingsresultat med lovvalgsperiode`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                fom = java.time.LocalDate.of(2023, 1, 1)
            }
        }

        behandlingsresultat.run {
            lovvalgsperioder shouldHaveSize 1
            lovvalgsperioder.first().run {
                fom shouldBe java.time.LocalDate.of(2023, 1, 1)
                behandlingsresultat shouldBe behandlingsresultat
            }
        }
    }

    @Test
    fun `Behandlingsresultat med årsavregning`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            årsavregning {
                aar = 2023
            }
        }

        behandlingsresultat.run {
            årsavregning shouldNotBe null
            årsavregning?.aar shouldBe 2023
            årsavregning?.behandlingsresultat shouldBe behandlingsresultat
        }
    }

    @Test
    fun `Behandlingsresultat med flere medlemskapsperioder`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            medlemskapsperiode {
                fom = java.time.LocalDate.of(2023, 1, 1)
                tom = java.time.LocalDate.of(2023, 6, 30)
            }
            medlemskapsperiode {
                fom = java.time.LocalDate.of(2023, 7, 1)
                tom = java.time.LocalDate.of(2023, 12, 31)
            }
        }

        behandlingsresultat.medlemskapsperioder shouldHaveSize 2
    }

    @Test
    fun `Behandlingsresultat med default ID constant`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BehandlingsresultatTestFactory.DEFAULT_ID
        }

        behandlingsresultat.id shouldBe BehandlingsresultatTestFactory.DEFAULT_ID
    }
}
