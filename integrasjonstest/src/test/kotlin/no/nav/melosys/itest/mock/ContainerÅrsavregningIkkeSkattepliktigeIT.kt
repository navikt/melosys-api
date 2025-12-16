package no.nav.melosys.itest.mock

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeFinner
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeProsessGenerator
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Container-based version of ÅrsavregningIkkeSkattepliktigeIT.
 * Uses melosys-mock container for external service mocking.
 */
class ContainerÅrsavregningIkkeSkattepliktigeIT(
    @Autowired private val årsavregningIkkeSkattepliktigeFinner: ÅrsavregningIkkeSkattepliktigeFinner,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val årsavregningIkkeSkattepliktigeProsessGenerator: ÅrsavregningIkkeSkattepliktigeProsessGenerator
) : ContainerMockServerTestBase() {

    @Nested
    @DisplayName("Prosessgenerering")
    inner class Prosessgenerering {
        @Test
        fun `skal opprette årsavregningsbehandling via prosessinstans når sak oppfyller krav`() {
            val sakOppfyllerKrav = "MEL-OPPFYLLER-KRAV-CONTAINER"

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
                medlemskapsperioder.clear() // TODO, finn en mer elegant måte for dette (Vi vil overstyre kun fom tom under)
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

            fagsakRepository.findBySaksnummer(sakOppfyllerKrav)
                .shouldBePresent().run {
                    withClue("Skal ha både førstegangsbehandling og årsavregning") {
                        behandlinger.shouldHaveSize(2)
                    }

                    val årsavregningsbehandling = behandlinger
                        .firstOrNull { it.type == Behandlingstyper.ÅRSAVREGNING }
                        .shouldNotBeNull()

                    behandlingsresultatRepository.findById(årsavregningsbehandling.id)
                        .shouldBePresent()
                        .årsavregning.shouldNotBeNull()
                        .aar shouldBe 2025
                }
        }
    }

    @Nested
    @DisplayName("Grunnleggende filtrering")
    inner class GrunnleggendeFiltrering {
        @Test
        fun `skal finne sak som oppfyller krav`() {
            val sakOppfyllerKrav = "MEL-OPPFYLLER-KRAV-CONTAINER"

            lagBehandlingsresultat {
                behandling {
                    status = Behandlingsstatus.AVSLUTTET
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
        fun `skal ikke finne sak med tidligere FASTSATT_TRYGDEAVGIFT behandling`() {
            val sakOppfyllerIkkeKrav = "MEL-OPPFYLLER-IKKE-KRAV-CONTAINER"

            lagBehandlingsresultat {
                behandling {
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
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
                    status = Behandlingsstatus.AVSLUTTET
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
    }

    @Nested
    @DisplayName("Skattepliktvurdering")
    inner class Skattepliktvurdering {
        @Test
        fun `skal ikke finne sak når nyeste vurdering endrer til skattepliktig`() {
            val sakOppfyllerIkkeKrav = "MEL-OPPFYLLER-IKKE-KRAV-CONTAINER"

            // Første vurdering: IKKE_SKATTEPLIKTIG
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
                medlemskapsperiode {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = FOM.minusYears(1)
                    tom = TOM
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
                    trygdeavgiftsperiode {
                        trygdeavgiftsbeløpMd = BigDecimal(500.0)
                        trygdesats = BigDecimal(50)
                        grunnlagSkatteforholdTilNorge {
                            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                        }
                    }
                }
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            }

            // Andre vurdering: SKATTEPLIKTIG (nyere - denne gjelder nå)
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
                medlemskapsperiode {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = FOM.minusYears(1)
                    tom = TOM
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
                    trygdeavgiftsperiode {
                        trygdeavgiftsbeløpMd = BigDecimal(500.0)
                        trygdesats = BigDecimal(50)
                        grunnlagSkatteforholdTilNorge {
                            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                        }
                    }
                }
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            }


            val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

            // Verifiser at saken IKKE ble funnet
            sakerMedBehandlinger.filter { it.sak.saksnummer == sakOppfyllerIkkeKrav }
                .shouldBeEmpty()
        }

        @Test
        fun `skal finne sak når nyeste vurdering endrer til ikke-skattepliktig`() {
            val sakOppfyllerKrav = "MEL-OPPFYLLER-KRAV-CONTAINER"

            // Første vurdering: SKATTEPLIKTIG (eldre)
            lagBehandlingsresultat {
                behandling {
                    status = Behandlingsstatus.AVSLUTTET
                    type = Behandlingstyper.FØRSTEGANG
                    fagsak {
                        saksnummer = sakOppfyllerKrav
                        type = Sakstyper.FTRL
                        status = Saksstatuser.LOVVALG_AVKLART
                    }
                }
                medlemskapsperiode {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = FOM.minusYears(1)
                    tom = TOM
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
                    trygdeavgiftsperiode {
                        trygdeavgiftsbeløpMd = BigDecimal(500.0)
                        trygdesats = BigDecimal(50)
                        grunnlagSkatteforholdTilNorge {
                            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                        }
                    }
                }
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            }

            // Andre vurdering: IKKE_SKATTEPLIKTIG (nyere - denne gjelder nå)
            lagBehandlingsresultat {
                behandling {
                    status = Behandlingsstatus.AVSLUTTET
                    type = Behandlingstyper.FØRSTEGANG
                    fagsak {
                        saksnummer = sakOppfyllerKrav
                        type = Sakstyper.FTRL
                        status = Saksstatuser.LOVVALG_AVKLART
                    }
                }
                medlemskapsperiode {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = FOM.minusYears(1)
                    tom = TOM
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
                    trygdeavgiftsperiode {
                        trygdeavgiftsbeløpMd = BigDecimal(500.0)
                        trygdesats = BigDecimal(50)
                        grunnlagSkatteforholdTilNorge {
                            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                        }
                    }
                }
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            }

            val sakerMedBehandlinger = årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(FOM, TOM)

            // Verifiser at saken ble funnet
            sakerMedBehandlinger.filter { it.sak.saksnummer == sakOppfyllerKrav }
                .shouldHaveSize(1)
                .single()
                .sak.saksnummer shouldBe sakOppfyllerKrav
        }
    }

    @Nested
    @DisplayName("Årsavregningsstatus")
    inner class Årsavregningsstatus {
        @Test
        fun `skal ikke finne sak med automatisk opprettet årsavregning for samme år`() {
            val sakOppfyllerIkkeKrav = "MEL-OPPFYLLER-IKKE-KRAV-CONTAINER"

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
        fun `skal ikke finne sak med manuelt opprettet årsavregning for samme år`() {
            val sakOppfyllerIkkeKrav = "MEL-MANUELL-ÅRSAVREGNING-CONTAINER"

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
        fun `skal ikke finne sak med avsluttet årsavregning for samme år`() {
            val sakOppfyllerIkkeKrav = "MEL-AVSLUTTET-ÅRSAVREGNING-CONTAINER"

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
    }

    @Nested
    @DisplayName("Periodeoverlapp")
    inner class Periodeoverlapp {
        @Test
        fun `skal finne sak når medlemskapsperiode starter før søkeperioden`() {
            val sakOppfyllerKrav = "MEL-OVERLAPP-TIDLIG-START-CONTAINER"

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
        fun `skal finne sak når medlemskapsperiode slutter etter søkeperioden`() {
            val sakOppfyllerKrav = "MEL-OVERLAPP-SEN-SLUTT-CONTAINER"

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
        fun `skal finne sak når medlemskapsperiode spenner over flere år`() {
            val sakOppfyllerKrav = "MEL-OVERLAPP-FLERE-ÅR-CONTAINER"

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
                    periodeFra = FOM
                    periodeTil = TOM
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
        private val ÅRSAVREGNING_ÅR = FOM.year
    }
}
