package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.vedtak.etterregistrering.EtterregistreringAvVedtaksmeldinger
import no.nav.melosys.service.vedtak.etterregistrering.EtterregistreringParametre
import no.nav.melosys.service.vedtak.etterregistrering.VedtaksmånedRad
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class EtterregistreringAvVedtaksmeldingerIT(
    @Autowired private val etterregistrering: EtterregistreringAvVedtaksmeldinger,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : ComponentTestBase() {

    @Test
    fun `simulering sender ingenting, og skarp kjøring sender én melding per behandling i utvalget i vedtaksrekkefølge`() {
        val sakMedKjentPerson = lagreSak("MEL-8361A", Sakstyper.FTRL, AKTØR_MED_IDENT)
        lagreBehandling(sakMedKjentPerson, periode("2023-01-01", "2023-12-31"), vedtaksdato = "2024-01-10")
        val starterI2023 = lagreBehandling(sakMedKjentPerson, periode("2023-06-01", "2024-05-31"), vedtaksdato = "2024-02-01")
        lagreBehandling(sakMedKjentPerson, periode("2024-01-01", "2024-12-31", InnvilgelsesResultat.AVSLAATT), vedtaksdato = "2024-03-01")
        lagreBehandling(sakMedKjentPerson, periode("2024-01-01", "2024-12-31"), status = Behandlingsstatus.UNDER_BEHANDLING)
        val førstVedtatt = lagreBehandling(sakMedKjentPerson, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-01-15")

        val sakUtenVedtaksdato = lagreSak("MEL-8361B", Sakstyper.FTRL, AKTØR_MED_IDENT)
        val utenVedtaksdato = lagreBehandling(sakUtenVedtaksdato, periode("2025-01-01", "2025-12-31"), vedtaksdato = null)
        lagreFerdigIverksetting(utenVedtaksdato, "2025-03-03 10:00:00")

        lagreBehandling(lagreSak("MEL-8361C", Sakstyper.EU_EOS, AKTØR_MED_IDENT), periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-01-20")

        val sakUtenIdent = lagreSak("MEL-8361D", Sakstyper.FTRL, AKTØR_UTEN_IDENT)
        lagreBehandling(sakUtenIdent, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-06-01")
        melosysHendelseKafkaConsumer.clear()

        etterregistrering.kjør(parametre(skarp = false))

        with(etterregistrering.rapport()) {
            sendteBehandlingIder shouldContainExactly listOf(førstVedtatt, starterI2023, utenVedtaksdato)
            perVedtaksmaaned shouldContainExactly listOf(
                VedtaksmånedRad("2024-01", iUtvalget = 1, sendt = 1, utenIdent = 0, feilet = 0),
                VedtaksmånedRad("2024-02", iUtvalget = 1, sendt = 1, utenIdent = 0, feilet = 0),
                VedtaksmånedRad("2024-06", iUtvalget = 1, sendt = 0, utenIdent = 1, feilet = 0),
                VedtaksmånedRad("2025-03", iUtvalget = 1, sendt = 1, utenIdent = 0, feilet = 0),
            )
            feil shouldBe emptyList()
        }
        etterregistrering.status()["antallUtenIdent"] shouldBe 1

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 10))

        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(førstVedtatt, starterI2023, utenVedtaksdato)
        await.atMost(Duration.ofSeconds(10)).until { melosysHendelseKafkaConsumer.melosysHendelser.size >= 3 }
        val meldinger = melosysHendelseKafkaConsumer.melosysHendelser.shouldHaveSize(3)
            .map { it.value().melding.shouldBeInstanceOf<VedtakHendelseMelding>() }
        meldinger.map { it.medlemskapsperioder } shouldContainExactly listOf(
            listOf(Periode(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"), InnvilgelsesResultat.INNVILGET)),
            listOf(Periode(LocalDate.parse("2023-06-01"), LocalDate.parse("2024-05-31"), InnvilgelsesResultat.INNVILGET)),
            listOf(Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31"), InnvilgelsesResultat.INNVILGET)),
        )
        meldinger.first() shouldBe VedtakHendelseMelding(
            folkeregisterIdent = FOLKEREGISTERIDENT,
            sakstype = Sakstyper.FTRL,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandligsresultatType = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK,
            medlemskapsperioder = listOf(Periode(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"), InnvilgelsesResultat.INNVILGET)),
            lovvalgsperioder = emptyList(),
        )
    }

    @Test
    fun `periodeTomFra flytter grensen for hvilke perioder som er med`() {
        val sak = lagreSak("MEL-8361E", Sakstyper.FTRL, AKTØR_MED_IDENT)
        lagreBehandling(sak, periode("2023-01-01", "2023-12-31"), vedtaksdato = "2024-01-10")
        val slutter2024 = lagreBehandling(sak, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-02-10")

        etterregistrering.kjør(parametre(skarp = false, periodeTomFra = LocalDate.of(2024, 12, 31)))
        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(slutter2024)

        etterregistrering.kjør(parametre(skarp = false, periodeTomFra = LocalDate.of(2025, 1, 1)))
        etterregistrering.rapport().sendteBehandlingIder shouldBe emptyList()
    }

    @Test
    fun `behandlinger som er henlagt, ikke har vedtak eller ligger i en annullert sak, er ikke med`() {
        val sak = lagreSak("MEL-8361F", Sakstyper.FTRL, AKTØR_MED_IDENT)
        val vedtatt = lagreBehandling(sak, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-04-01")
        lagreBehandling(sak, periode("2024-01-01", "2024-12-31"), vedtaksdato = null)
        listOf(
            Behandlingsresultattyper.HENLEGGELSE,
            Behandlingsresultattyper.HENLEGGELSE_BORTFALT,
            Behandlingsresultattyper.ANNULLERT,
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
            Behandlingsresultattyper.FERDIGBEHANDLET,
        ).forEach { lagreBehandling(sak, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-05-01", resultattype = it) }

        val annullertSak = lagreSak("MEL-8361G", Sakstyper.FTRL, AKTØR_MED_IDENT, Saksstatuser.ANNULLERT)
        lagreBehandling(annullertSak, periode("2024-01-01", "2024-12-31"), vedtaksdato = "2024-06-01")

        etterregistrering.kjør(parametre(skarp = false))

        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(vedtatt)
    }

    private fun parametre(skarp: Boolean, maksAntall: Int? = null, periodeTomFra: LocalDate = LocalDate.of(2024, 1, 1)) =
        EtterregistreringParametre(periodeTomFra = periodeTomFra, maksAntall = maksAntall, maksFeil = 100, skarp = skarp)

    private data class TestPeriode(val fom: LocalDate, val tom: LocalDate, val resultat: InnvilgelsesResultat)

    private fun periode(fom: String, tom: String, resultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET) =
        TestPeriode(LocalDate.parse(fom), LocalDate.parse(tom), resultat)

    private fun lagreSak(
        saksnummer: String,
        sakstype: Sakstyper,
        aktørId: String,
        saksstatus: Saksstatuser = FagsakTestFactory.SAKSSTATUS,
    ): Fagsak = fagsakRepository.save(
        Fagsak.forTest {
            this.saksnummer = saksnummer
            type = sakstype
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = saksstatus
            medBruker { this.aktørId = aktørId }
        }
    )

    private fun lagreBehandling(
        fagsak: Fagsak,
        periode: TestPeriode,
        vedtaksdato: String? = null,
        status: Behandlingsstatus = Behandlingsstatus.AVSLUTTET,
        resultattype: Behandlingsresultattyper = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
    ): Long = behandlingsresultatRepository.saveAndFlush(
        Behandlingsresultat.forTest {
            type = resultattype
            behandling {
                medFagsak(fagsak)
                type = Behandlingstyper.FØRSTEGANG
                this.status = status
            }
            medlemskapsperiode {
                fom = periode.fom
                tom = periode.tom
                innvilgelsesresultat = periode.resultat
            }
            if (vedtaksdato != null) {
                vedtakMetadata {
                    this.vedtaksdato = Instant.parse("${vedtaksdato}T12:00:00Z")
                    vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                }
            }
        }
    ).hentBehandling().id

    private fun lagreFerdigIverksetting(behandlingId: Long, registrertDato: String) {
        jdbcTemplate.update(
            """INSERT INTO prosessinstans (uuid, prosess_type, status, behandling_id, registrert_dato, endret_dato, prioritet)
               VALUES (SYS_GUID(), 'IVERKSETT_VEDTAK_FTRL', 'FERDIG', ?, ?, ?, 'NORMAL')""",
            behandlingId, java.sql.Timestamp.valueOf(registrertDato), java.sql.Timestamp.valueOf(registrertDato)
        )
    }

    private companion object {
        // Personene i melosys-mock: 1111111111111 har folkeregisterident 30056928150, den andre finnes ikke.
        const val AKTØR_MED_IDENT = "1111111111111"
        const val AKTØR_UTEN_IDENT = "1234567890123"
        const val FOLKEREGISTERIDENT = "30056928150"
    }
}
