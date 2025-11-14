package no.nav.melosys.itest

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.forTest
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
import org.assertj.core.api.Assertions.assertThat
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
        val behandlingsresultat = lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift()
        val nyLovvalgsperiode = nyLovvalgsperiodeUtenTrygdeavgift().apply {
            setLovvalgsland(Land_iso2.SE)
            setMedlPeriodeID(321L)
        }

        val resultat = lovvalgsperiodeService.lagreLovvalgsperioder(
            behandlingsresultat.hentId(),
            listOf(nyLovvalgsperiode)
        )

        assertThat(resultat).hasSize(1)
        val lagret = resultat.single()
        assertThat(lagret.lovvalgsland).isEqualTo(Land_iso2.SE)
        assertThat(lagret.medlPeriodeID).isEqualTo(321L)

        val lagredeFraRepo = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandlingsresultat.hentId())
        assertThat(lagredeFraRepo).hasSize(1)
        assertThat(lagredeFraRepo.single().lovvalgsland).isEqualTo(Land_iso2.SE)
        assertThat(lagredeFraRepo.single().trygdeavgiftsperioder).isEmpty()
    }

    private fun lagreBehandlingsresultatMedLovvalgsperiodeSomHarTrygdeavgift(): Behandlingsresultat {
        val behandling = lagreBehandling()

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            leggTilRegisteringInfo()
        }

        val lagretBehandlingsresultat = behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
        lagreLovvalgsperiodeMedTrygdeavgiftsperiode(lagretBehandlingsresultat)

        return lagretBehandlingsresultat
    }

    private fun lagreBehandling(): Behandling {
        val fagsak = Fagsak(
            "MEL-${UUID.randomUUID()}",
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET
        ).apply {
            leggTilRegisteringInfo()
        }

        val lagretFagsak = fagsakRepository.save(fagsak)

        val behandling = Behandling(
            id = 0,
            fagsak = lagretFagsak,
            status = Behandlingsstatus.UNDER_BEHANDLING,
            type = Behandlingstyper.FØRSTEGANG,
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            behandlingsfrist = LocalDate.now().plusMonths(6)
        ).apply {
            leggTilRegisteringInfo()
        }

        return behandlingRepository.save(behandling)
    }

    private fun lagreLovvalgsperiodeMedTrygdeavgiftsperiode(behandlingsresultat: Behandlingsresultat) {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            setBehandlingsresultat(behandlingsresultat)
            setFom(LocalDate.now().minusMonths(6))
            setTom(LocalDate.now().minusMonths(3))
            setLovvalgsland(Land_iso2.NO)
            setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            setDekning(Trygdedekninger.FULL_DEKNING)
            setMedlPeriodeID(123L)
        }

        lovvalgsperiode.addTrygdeavgiftsperiode(
            Trygdeavgiftsperiode.forTest {
                periodeFra = LocalDate.now().minusMonths(6)
                periodeTil = LocalDate.now().minusMonths(5)
                trygdeavgiftsbeløpMd = BigDecimal.valueOf(1000)
                trygdesats = BigDecimal.ONE
            }
        )

        lovvalgsperiodeRepository.saveAndFlush(lovvalgsperiode)
    }

    private fun nyLovvalgsperiodeUtenTrygdeavgift() = Lovvalgsperiode().apply {
        setFom(LocalDate.now())
        setTom(LocalDate.now().plusMonths(6))
        setLovvalgsland(Land_iso2.NO)
        setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E)
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        setDekning(Trygdedekninger.FULL_DEKNING)
        setMedlPeriodeID(999L)
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        registrertAv = "test"
        endretAv = "test"
    }
}
