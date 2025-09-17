package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.behandlingsresultatForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.ftrl.FinnSakerÅrsavregningIkkeSkattepliktige
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant.parse
import java.time.LocalDate

class FinnSakerÅrsavregningIkkeSkattepliktigeIT(
    @Autowired private val finnSakerÅrsavregningIkkeSkattepliktige: FinnSakerÅrsavregningIkkeSkattepliktige,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
) : ComponentTestBase() {

    @BeforeEach
    fun setupTestData() {
        val behandlingsresultat = lagBehandlingsresultatIkkeSkattepliktige()

        fagsakRepository.save(behandlingsresultat.behandling.fagsak)
        addCleanUpAction { slettSakMedAvhengigheter(behandlingsresultat.behandling.fagsak.saksnummer) }

        behandlingsresultatRepository.save(behandlingsresultat)
    }

    @Test
    fun `finn saker for årsavregning ikke skattepliktige - skal finne registert sak som oppfyller krav`() {
        finnSakerÅrsavregningIkkeSkattepliktige.finnSaker(
            dryrun = true,
            antallFeilFørStopAvJob = 0,
            fomDato = LocalDate.of(2024, 1, 1),
            tomDato = LocalDate.of(2024, 12, 31)
        )

        finnSakerÅrsavregningIkkeSkattepliktige.sakerFunnet
            .shouldHaveSize(1)
            .single()
            .sak.saksnummer shouldBe SAK
    }

    private fun lagBehandlingsresultatIkkeSkattepliktige() =
        behandlingsresultatForTest {
            behandling {
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAK
                    type = Sakstyper.FTRL
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                    status = Saksstatuser.LOVVALG_AVKLART
                    medBruker()
                }
            }
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            fastsattAvLand = Land_iso2.NO
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG

            vedtakMetadata {
                vedtaksdato = parse("2002-02-11T09:37:30Z")
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
                        grunnlagInntekstperiode = Inntektsperiode().apply {
                            fomDato = FOM
                            tomDato = TOM
                            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                            avgiftspliktigMndInntekt = Penger(1000.0)
                            isArbeidsgiversavgiftBetalesTilSkatt = false
                        },
                        grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                            fomDato = FOM
                            tomDato = TOM
                            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                        }
                    )
                )
            }
        }

    companion object {
        const val SAK = "MEL-IKKE-SKATTEPLIKTIG-1"
        private val FOM = LocalDate.of(2024, 1, 1)
        private val TOM = LocalDate.of(2024, 12, 31)
    }
}
