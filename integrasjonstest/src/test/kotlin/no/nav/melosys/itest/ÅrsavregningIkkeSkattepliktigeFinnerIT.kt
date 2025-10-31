package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
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
    fun `skal ikke finne sak med ny automatisk opprettet årsavregningsbehandling`() {
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
            årsavregning {
                aar = ÅRSAVREGNING_ÅR
            }
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

    @Test
    fun `skal ikke finne sak med manuelt opprettet årsavregning`() {
        val sakOppfyllerIkkeKrav = "MEL-MANUELL-ÅRSAVREGNING"

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
            årsavregning {
                aar = ÅRSAVREGNING_ÅR
            }
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
                medBehandlingsårsakType(Behandlingsaarsaktyper.ANNET)
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
    fun `skal ikke finne sak med avsluttet automatisk opprettet årsavregning`() {
        val sakOppfyllerIkkeKrav = "MEL-AVSLUTTET-ÅRSAVREGNING"

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
            årsavregning {
                aar = ÅRSAVREGNING_ÅR
            }
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
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

    @Test
    fun `skal finne sak hvor medlemskapsperiode starter tidligere enn fomDato men overlapper med perioden`() {
        val sakOppfyllerKrav = "MEL-OVERLAPP-TIDLIG-START"

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = sakOppfyllerKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN

            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = FOM.minusYears(1)
                tom = TOM
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    trygdesats = BigDecimal(50)
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

        sakerMedBehandlinger.single { it.sak.saksnummer == sakOppfyllerKrav }
            .sak.saksnummer shouldBe sakOppfyllerKrav
    }

    @Test
    fun `skal finne sak hvor medlemskapsperiode slutter senere enn tomDato men overlapper med perioden`() {
        val sakOppfyllerKrav = "MEL-OVERLAPP-SEN-SLUTT"

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = sakOppfyllerKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN

            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = FOM
                tom = TOM.plusYears(1)
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    trygdesats = BigDecimal(50)
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

        sakerMedBehandlinger.single { it.sak.saksnummer == sakOppfyllerKrav }
            .sak.saksnummer shouldBe sakOppfyllerKrav
    }

    @Test
    fun `skal finne sak hvor medlemskapsperiode dekker flere år`() {
        val sakOppfyllerKrav = "MEL-OVERLAPP-FLERE-ÅR"

        lagBehandlingsresultat {
            behandling {
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = sakOppfyllerKrav
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN

            medlemskapsperiode {
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = FOM.minusYears(1)
                tom = TOM.plusYears(1)
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    trygdesats = BigDecimal(50)
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

        sakerMedBehandlinger.single { it.sak.saksnummer == sakOppfyllerKrav }
            .sak.saksnummer shouldBe sakOppfyllerKrav
    }

    @Test
    fun `skal finne sak for nytt år selv om saken har årsavregning for tidligere år`() {
        val sakOppfyllerIkkeKrav = "MEL-ÅRSAVREGNING-FOR-ANNET-ÅR"

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
            årsavregning {
                aar = ÅRSAVREGNING_ÅR - 1 // forrige år for å teste at vi fortsatt finner for inneværende år
            }
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
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
            .shouldHaveSize(1).single()
            .sak.saksnummer shouldBe sakOppfyllerIkkeKrav
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
                fom = FOM
                tom = TOM
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    trygdesats = BigDecimal(50)
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            block()
        }.also {
            fagsakRepository.save(it.hentBehandling().fagsak)
            addCleanUpAction { slettSakMedAvhengigheter(it.hentBehandling().fagsak.saksnummer) }
            behandlingsresultatRepository.save(it)
        }

    companion object {
        private val FOM = LocalDate.of(LocalDate.now().year, 1, 1)
        private val TOM = LocalDate.of(LocalDate.now().year, 12, 31)
        private val ÅRSAVREGNING_ÅR = FOM.year
    }
}
