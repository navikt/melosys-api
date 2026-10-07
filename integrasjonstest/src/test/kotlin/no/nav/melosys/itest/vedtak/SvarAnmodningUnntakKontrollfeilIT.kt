package no.nav.melosys.itest.vedtak

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.itest.ComponentTestBase
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.steg.sed.BestemBehandlingsmåteSvarAnmodningUnntak
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.validering.Kontrollfeil
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/**
 * Steget fanger kontrollfeilen fra vedtaksfattingen og committer statusendringen. Det som står igjen i
 * databasen etter commit, kan bare vises med ekte transaksjoner.
 */
class SvarAnmodningUnntakKontrollfeilIT(
    @Autowired private val steg: BestemBehandlingsmåteSvarAnmodningUnntak,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : ComponentTestBase() {

    @MockkBean
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade

    @Test
    fun `kontrollfeil ved automatisk vedtak gir svar mottatt og beholder resultattypen`() {
        val behandlingId = lagBehandlingMedInnvilgetAnmodning()
        every { ferdigbehandlingKontrollFacade.kontroller(any(), true, any(), any()) } returns emptyList()
        every {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(any(), any(), any(), any())
        } returns listOf(Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER))

        steg.utfør(lagProsessinstans(behandlingId))

        verify(exactly = 1) {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any(), Sakstyper.EU_EOS, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, any()
            )
        }
        behandlingRepository.findById(behandlingId).get().status shouldBe Behandlingsstatus.SVAR_ANMODNING_MOTTATT
        behandlingsresultatRepository.findById(behandlingId).get().type shouldBe Behandlingsresultattyper.ANMODNING_OM_UNNTAK
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM vedtak_metadata WHERE behandlingsresultat_id = ?", Int::class.java, behandlingId
        ) shouldBe 0
    }

    private fun lagBehandlingMedInnvilgetAnmodning(): Long {
        val fagsak = fagsakRepository.save(
            Fagsak.forTest {
                saksnummer = "MEL-8307"
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                medBruker()
            }
        )
        val behandlingsresultat = Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
            behandling {
                medFagsak(fagsak)
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            }
            anmodningsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                sendtUtland = true
                anmodningsperiodeSvar {
                    anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
                    registrertDato = LocalDate.now()
                }
            }
        }
        return behandlingsresultatRepository.saveAndFlush(behandlingsresultat).hentBehandling().id
    }

    private fun lagProsessinstans(behandlingId: Long) = Prosessinstans.forTest {
        type = ProsessType.ANMODNING_OM_UNNTAK_SVAR
        behandling { id = behandlingId }
        medData(
            ProsessDataKey.EESSI_MELDING,
            MelosysEessiMelding(svarAnmodningUnntak = SvarAnmodningUnntak(beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE))
        )
    }
}
