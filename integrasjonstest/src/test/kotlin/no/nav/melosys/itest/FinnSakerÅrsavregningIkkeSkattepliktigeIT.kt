package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.Application
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.ftrl.FinnSakerÅrsavregningIkkeSkattepliktige
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import java.time.Instant.parse
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local", "teammelosys.fattetvedtak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
class FinnSakerÅrsavregningIkkeSkattepliktigeIT(
    @Autowired private val finnSakerÅrsavregningIkkeSkattepliktige: FinnSakerÅrsavregningIkkeSkattepliktige,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
) : OracleTestContainerBase() {

    @BeforeEach
    fun setupTestData() {
        createTestDataForIkkeSkattepliktige()
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

    private fun createTestDataForIkkeSkattepliktige() {
        Behandlingsresultat().apply br@{
            behandling = Behandling.forTest {
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAK
                    type = Sakstyper.FTRL
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                    status = Saksstatuser.LOVVALG_AVKLART
                    this.medBruker()
                }
            }.also {
                fagsakRepository.save(it.fagsak)
                addCleanUpAction { slettSakMedAvhengigheter(it.fagsak.saksnummer) }
            }
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            fastsattAvLand = Land_iso2.NO
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
            leggTilRegisteringInfo()
            vedtakMetadata = VedtakMetadata().apply {
                behandlingsresultat = this@br
                vedtaksdato = parse("2002-02-11T09:37:30Z")
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                leggTilRegisteringInfo()
            }
            addMedlemskapsperiode(Medlemskapsperiode().apply {
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
                        },
                        grunnlagMedlemskapsperiode = this
                    )
                )
            })
        }.also {
            behandlingsresultatRepository.save(it)
        }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }

    companion object {
        const val SAK = "MEL-IKKE-SKATTEPLIKTIG-1"
        private val FOM = LocalDate.of(2024, 1, 1)
        private val TOM = LocalDate.of(2024, 12, 31)
    }
}
