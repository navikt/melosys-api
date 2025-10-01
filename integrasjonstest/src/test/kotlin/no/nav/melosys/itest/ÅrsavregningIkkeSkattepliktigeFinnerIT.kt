package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeFinner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ÅrsavregningIkkeSkattepliktigeFinnerIT(
    @Autowired private val årsavregningIkkeSkattepliktigeFinner: ÅrsavregningIkkeSkattepliktigeFinner,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
) : ComponentTestBase() {

    @Test
    fun `skal finne registrert sak som oppfyller krav`() {
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

        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

        sakerMedBehandlinger.filter { it.sak.saksnummer == sakOppfyllerKrav }
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


        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)


        sakerMedBehandlinger.filter { it.sak.saksnummer == sakOppfyllerIkkeKrav }
            .shouldBeEmpty()
    }

    @Test
    fun `skal ikke finne registert sak hvor vi har opprettet ny årsavregnings behandling`() {
        val sakOppfyllerIkkeKrav = "MEL-OPPFYLLER-IKKE-KRAV"

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = sakOppfyllerIkkeKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        }

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
                medBehandlingsårsakType(Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE)
                fagsak {
                    saksnummer = sakOppfyllerIkkeKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        }


        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)


        sakerMedBehandlinger.filter { it.sak.saksnummer == sakOppfyllerIkkeKrav }
            .shouldBeEmpty()
    }


    private fun lagBehandlingsresultat(block: Behandlingsresultat.() -> Unit = {}) =
        behandlingsresultatForTest {
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
        private val FOM = LocalDate.of(LocalDate.now().year, 1, 1)
        private val TOM = LocalDate.of(LocalDate.now().year, 12, 31)
    }
}
