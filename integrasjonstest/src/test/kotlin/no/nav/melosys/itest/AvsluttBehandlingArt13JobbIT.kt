package no.nav.melosys.itest

import io.mockk.spyk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.jobb.AvsluttArt13BehandlingJobb
import no.nav.melosys.service.behandling.jobb.AvsluttArt13BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test

class AvsluttBehandlingArt13JobbIT(
    @Autowired val behandlingService: BehandlingService,
    @Autowired val avsluttArt13BehandlingService: AvsluttArt13BehandlingService,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    @Autowired val mottatteOpplysningerService: MottatteOpplysningerService,

) : ComponentTestBase() {
    val saksnummer = "MEL-aktoerhistorikk"

    private lateinit var avsluttArt13BehandlingJobb: AvsluttArt13BehandlingJobb

    @BeforeEach
    fun setup() {
        avsluttArt13BehandlingJobb = AvsluttArt13BehandlingJobb(behandlingService, avsluttArt13BehandlingService)
    }

    @Test
    fun testAvsluttBehandlingArt13Jobb() {
        val behandlingServiceSpy = spyk(behandlingService)
        val jobb = AvsluttArt13BehandlingJobb(behandlingServiceSpy, avsluttArt13BehandlingService)

        val fagsak = lagFagsak("test")
        val aktoer = Aktoer().apply {
            this.fagsak = fagsak
            rolle = Aktoersroller.BRUKER
            personIdent = "21075114491"
            aktørId = "123456"
        }
        fagsak.leggTilAktør(aktoer)
        fagsakRepository.save(fagsak)

        val behandling = behandlingService.nyBehandling(
            fagsak,
            Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR,
            "test",
            "test",
            LocalDate.now(),
            Behandlingsaarsaktyper.SED,
            "test",
        )

        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.soeknadsland = Soeknadsland.av(Land_iso2.AT)

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
            this.behandling = behandling
            this.registrertDato = Instant.now()
            this.endretDato = Instant.now()
            this.type = Mottatteopplysningertyper.SED
            this.versjon = "1.0"
        }

        no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)
        behandling.mottatteOpplysninger = mottatteOpplysninger
        behandling.saksopplysninger.add(Saksopplysning().apply {
            type = SaksopplysningType.PDL_PERSOPL
            this.behandling = behandling
            versjon = "1.0"
            registrertDato = Instant.now()
            endretDato = Instant.now()
            dokument = no.nav.melosys.domain.dokument.person.PersonDokument()
        })
        behandling.saksopplysninger.add(Saksopplysning().apply {
            type = SaksopplysningType.PDL_PERS_SAKS
            this.behandling = behandling
            versjon = "1.0"
            registrertDato = Instant.now()
            endretDato = Instant.now()
            dokument = no.nav.melosys.domain.dokument.person.PersonDokument()
        })
        behandlingService.lagre(behandling)

        val behandlingsresultat = behandlingsresultatRepository.findById(behandling.id).orElseThrow()
        behandlingsresultat.settVedtakMetadata(LocalDate.now().plusDays(30))
        behandlingsresultat.vedtakMetadata.vedtaksdato = Instant.now().minusSeconds(7884000L) // ~3 måneder

        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Land_iso2.AT,
            null,
            null
        )

        utpekingsperiode.behandlingsresultat = behandlingsresultat
        behandlingsresultat.utpekingsperioder = setOf(utpekingsperiode)

        val lovvalgsperiode = Lovvalgsperiode().apply {
            this.behandlingsresultat = behandlingsresultat
            fom = LocalDate.now()
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            dekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
            medlPeriodeID = 1242L
            lovvalgsland = Land_iso2.AT
        }
        lovvalgsperiodeRepository.save(lovvalgsperiode)

        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)

        jobb.avsluttBehandlingArt13()

        val result = behandlingServiceSpy.hentBehandlingIderMedStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
        assert(result.isNotEmpty()) { "Expected at least one behandling ID" }

        val processedBehandling = behandlingService.hentBehandling(behandling.id)
        assert(processedBehandling.status == Behandlingsstatus.AVSLUTTET) {
            "Expected behandling ${behandling.id} to be AVSLUTTET, but was ${processedBehandling.status}"
        }
        }

    private fun lagFagsak(saksnummer: String): Fagsak {
        return Fagsak(
            saksnummer, null, Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.OPPRETTET
        ).apply { leggTilRegisteringInfo() }
            .also { fagsakRepository.save(it) }
            .also {
                addCleanUpAction {
                    slettSakMedAvhengigheter(it.saksnummer)
                }
            }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now().minusSeconds(7884000L)
        endretDato = Instant.now().minusSeconds(7884000L)
        endretAv = "Ikke Øystein"
    }
}
