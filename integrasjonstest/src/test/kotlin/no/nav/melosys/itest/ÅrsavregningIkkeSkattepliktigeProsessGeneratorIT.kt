package no.nav.melosys.itest

import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeProsessGenerator
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ÅrsavregningIkkeSkattepliktigeProsessGeneratorIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val årsavregningIkkeSkattepliktigeProsessGenerator: ÅrsavregningIkkeSkattepliktigeProsessGenerator

) : MockServerTestBaseWithProsessManager() {

    @Test
    fun `skal finne registrert sak som oppfyller krav`() {
        val sakOppfyllerKrav = "MEL-OPPFYLLER-KRAV"

        lagBehandlingsresultat {
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = sakOppfyllerKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING to 1
            )
        ) {
            // unngår problem med dobbelt registrering av siden dette også registreres i finnSakerOgLagProsessinstanser
            ThreadLocalAccessInfo.afterExecuteProcess(randomUUID)
            årsavregningIkkeSkattepliktigeProsessGenerator.finnSakerOgLagProsessinstanser(
                dryrun = false,
                antallFeilFørStopAvJob = 0,
                fomDato = FOM,
                tomDato = TOM,
            )
            ThreadLocalAccessInfo.beforeExecuteProcess(randomUUID, "steg")
        }
    }

    private fun lagBehandlingsresultat(block: BehandlingsresultatTestFactory.Builder.() -> Unit = {}) =
        Behandlingsresultat.forTest {
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
                fom = FOM.minusYears(1) // Test at periode kan starte før
                tom = TOM.plusYears(1) // Test at periode kan slutte etter
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    periodeFra = FOM.minusYears(1)
                    periodeTil = TOM.plusYears(1)
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    trygdesats = BigDecimal(50)
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            block()
        }.also {
            val fagsak = it.hentBehandling().fagsak
            // Clear bidirectional relationship to prevent Hibernate cascade conflicts
            // when saving Fagsak and Behandlingsresultat separately
            it.hentBehandling().fagsak.behandlinger.clear()

            fagsakRepository.save(fagsak)
            addCleanUpAction { slettSakMedAvhengigheter(it.hentBehandling().fagsak.saksnummer) }
            behandlingsresultatRepository.save(it)
        }

    companion object {
        private val FOM = LocalDate.of(LocalDate.now().year, 1, 1)
        private val TOM = LocalDate.of(LocalDate.now().year, 12, 31)
    }
}
