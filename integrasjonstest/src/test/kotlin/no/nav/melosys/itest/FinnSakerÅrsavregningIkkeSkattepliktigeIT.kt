package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.behandlingsresultatForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.ftrl.FinnSakerÅrsavregningIkkeSkattepliktige
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinnSakerÅrsavregningIkkeSkattepliktigeIT(
    @Autowired private val finnSakerÅrsavregningIkkeSkattepliktige: FinnSakerÅrsavregningIkkeSkattepliktige,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
) : ComponentTestBase() {

    @Test
    fun `skal finne registert sak som oppfyller krav`() {
        val sakOppfyllerKrav = "MEL-OPPFYLLER-KRAV"

        lagBehandlingsresultat {
            behandling {
                fagsak {
                    saksnummer = sakOppfyllerKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        }

        finnSakerÅrsavregningIkkeSkattepliktige.finnSaker(
            dryrun = true,
            antallFeilFørStopAvJob = 0,
            fomDato = LocalDate.of(2024, 1, 1),
            tomDato = LocalDate.of(2024, 12, 31)
        )

        finnSakerÅrsavregningIkkeSkattepliktige.sakerFunnet.filter { it.sak.saksnummer == sakOppfyllerKrav }
            .shouldHaveSize(1)
            .single()
            .sak.saksnummer shouldBe sakOppfyllerKrav
    }

    @Test
    fun `skal ikke finne registert sak hvor vi har tidligere behandling med FASTSATT_TRYGDEAVGIFT`() {
        val sakOppfyllerIkkeKrav = "MEL-OPPFYLLER-IKKE-KRAV"

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak {
                    saksnummer = sakOppfyllerIkkeKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
        }

        lagBehandlingsresultat {
            behandling {
                fagsak {
                    saksnummer = sakOppfyllerIkkeKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        }


        finnSakerÅrsavregningIkkeSkattepliktige.finnSaker(
            dryrun = true,
            antallFeilFørStopAvJob = 0,
            fomDato = LocalDate.of(2024, 1, 1),
            tomDato = LocalDate.of(2024, 12, 31)
        )

        finnSakerÅrsavregningIkkeSkattepliktige.sakerFunnet.filter { it.sak.saksnummer == sakOppfyllerIkkeKrav }
            .shouldBeEmpty()
    }

    private fun lagBehandlingsresultat(block: Behandlingsresultat.() -> Unit = {}) =
        behandlingsresultatForTest {
            behandling { }
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            fastsattAvLand = Land_iso2.NO
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG

            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            }

            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = FOM
                tom = TOM
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = FOM,
                        periodeTil = TOM,
                        trygdeavgiftsbeløpMd = Penger(500.0),
                        trygdesats = BigDecimal(50),
                        grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                            fomDato = FOM
                            tomDato = TOM
                            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                        }
                    )
                )
            }
            block()
        }.also {
            fagsakRepository.save(it.behandling.fagsak)
            addCleanUpAction { slettSakMedAvhengigheter(it.behandling.fagsak.saksnummer) }
            behandlingsresultatRepository.save(it)
        }

    companion object {
        private val FOM = LocalDate.of(2024, 1, 1)
        private val TOM = LocalDate.of(2024, 12, 31)
    }
}
