package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository
import no.nav.melosys.service.LovvalgsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class LovvalgsperiodeServiceIT(
    @Autowired
    private val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired
    private val tidligereMedlemsperiodeRepository: TidligereMedlemsperiodeRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository
) : DataJpaTestBase() {

    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @BeforeEach
    fun setUp() {
        lovvalgsperiodeService = LovvalgsperiodeService(
            behandlingsresultatRepository,
            lovvalgsperiodeRepository,
            tidligereMedlemsperiodeRepository,
            behandlingRepository
        )
    }

    @Test
    fun `lagreLovvalgsperioder erstatter periode selv om eksisterende har trygdeavgift`() {
        val (behandlingsresultat, _) = lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift()
        val nyLovvalgsperiode = nyLovvalgsperiodeUtenTrygdeavgift().apply {
            setLovvalgsland(Land_iso2.SE)
            setMedlPeriodeID(321L)
        }

        val resultat = lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiode)
        )

        resultat shouldHaveSize 1
        val lagret = resultat.single()
        lagret.lovvalgsland shouldBe Land_iso2.SE
        lagret.medlPeriodeID shouldBe 321L

        val lagredeFraRepo = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandlingsresultat.hentId())
        lagredeFraRepo shouldHaveSize 1
        lagredeFraRepo.single().lovvalgsland shouldBe Land_iso2.SE
        lagredeFraRepo.single().trygdeavgiftsperioder shouldHaveSize 1
    }

    @Test
    fun `lagreLovvalgsperioder kopierer trygdeavgiftsperioder med skatteforhold og inntektsperiode`() {
        val (behandlingsresultat, originalLovvalgsperiode) = lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift(
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        )

        val originalTrygdeavgift = originalLovvalgsperiode.trygdeavgiftsperioder.single()

        val originalTrygdeavgiftId = originalTrygdeavgift.id
        val originalSkatteforholdId = originalTrygdeavgift.grunnlagSkatteforholdTilNorge?.id
        val originalInntektsperiodeId = originalTrygdeavgift.grunnlagInntekstperiode?.id

        val resultat = lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiodeUtenTrygdeavgift())
        )

        resultat shouldHaveSize 1

        val lagretLovvalgsperiode = lovvalgsperiodeRepository
            .findByBehandlingsresultatId(behandlingsresultat.hentId())
            .single()

        val lagretTrygdeavgift = lagretLovvalgsperiode.trygdeavgiftsperioder.single()

        lagretTrygdeavgift.id shouldNotBe originalTrygdeavgiftId
        lagretTrygdeavgift.grunnlagLovvalgsPeriode shouldBe lagretLovvalgsperiode

        lagretTrygdeavgift.grunnlagSkatteforholdTilNorge.shouldNotBeNull()
            .id shouldNotBe originalSkatteforholdId

        lagretTrygdeavgift.grunnlagInntekstperiode.shouldNotBeNull()
            .id shouldNotBe originalInntektsperiodeId
    }

    private fun lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift(
        behandlingstema: Behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    ): LagretLovvalgsperiodeMedResultat {
        val lagretBehandling = lagreBehandling(behandlingstema)

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = lagretBehandling
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }.also {
            it.leggTilRegisteringInfo()
        }

        val lagretBehandlingsresultat = behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
        val lagretLovvalgsperiode = lagreLovvalgsperiodeMedTrygdeavgiftsperiode(lagretBehandlingsresultat)
        lagretBehandlingsresultat.lovvalgsperioder.add(lagretLovvalgsperiode)

        return LagretLovvalgsperiodeMedResultat(
            behandlingsresultat = lagretBehandlingsresultat,
            lovvalgsperiode = lagretLovvalgsperiode
        )
    }

    private fun lagreBehandling(
        behandlingstema: Behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    ): Behandling {
        val fagsak = Fagsak.forTest {
            saksnummer = "MEL-${UUID.randomUUID()}"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
        }.also {
            it.leggTilRegisteringInfo()
        }

        val lagretFagsak = fagsakRepository.save(fagsak)

        val behandling = Behandling.forTest {
            id = 0
            this.fagsak = lagretFagsak
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = behandlingstema
            behandlingsfrist = LocalDate.now().plusMonths(6)
        }.also {
            it.leggTilRegisteringInfo()
        }

        return behandlingRepository.save(behandling)
    }

    private fun lagreLovvalgsperiodeMedTrygdeavgiftsperiode(
        lagretBehandlingsresultat: Behandlingsresultat
    ): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode.forTest {
            behandlingsresultat = lagretBehandlingsresultat
            fom = LocalDate.now().minusMonths(6)
            tom = LocalDate.now().minusMonths(3)
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING
            medlPeriodeID = 123L
        }.apply {
            addTrygdeavgiftsperiode(
                Trygdeavgiftsperiode.forTest {
                    periodeFra = LocalDate.now().minusMonths(6)
                    periodeTil = LocalDate.now().minusMonths(5)
                    trygdeavgiftsbeløpMd = BigDecimal.valueOf(1000)
                    trygdesats = BigDecimal.ONE
                }
            )
        }

        return lovvalgsperiodeRepository.saveAndFlush(lovvalgsperiode)
    }

    private fun nyLovvalgsperiodeUtenTrygdeavgift() = Lovvalgsperiode.forTest {
        fom = LocalDate.now()
        tom = LocalDate.now().plusMonths(6)
        lovvalgsland = Land_iso2.NO
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        dekning = Trygdedekninger.FULL_DEKNING
        medlPeriodeID = 999L
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        registrertAv = "test"
        endretAv = "test"
    }

    private data class LagretLovvalgsperiodeMedResultat(
        val behandlingsresultat: Behandlingsresultat,
        val lovvalgsperiode: Lovvalgsperiode
    )
}
