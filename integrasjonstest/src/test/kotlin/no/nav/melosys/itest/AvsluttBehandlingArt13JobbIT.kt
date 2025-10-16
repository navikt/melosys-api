package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerKonverterer
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.Sporingsinformasjon
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.repository.BehandlingRepository
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
import java.time.LocalDateTime
import kotlin.test.Test

class AvsluttBehandlingArt13JobbIT(
    @Autowired val behandlingService: BehandlingService,
    @Autowired val avsluttArt13BehandlingService: AvsluttArt13BehandlingService,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    @Autowired val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired val behandlingRepository: BehandlingRepository,

    ) : ComponentTestBase() {
    private lateinit var avsluttArt13BehandlingJobb: AvsluttArt13BehandlingJobb

    @BeforeEach
    fun setup() {
        avsluttArt13BehandlingJobb = AvsluttArt13BehandlingJobb(behandlingService, avsluttArt13BehandlingService)
        MedlRepo.repo.apply {
            put(1242L, MedlemskapsunntakForGet().apply {
                unntakId = 1242L
                ident = "21075114491"
                fraOgMed = LocalDate.now()
                tilOgMed = LocalDate.now().plusYears(1)
                status = "GODKJENT"
                dekning = "FULL"
                lovvalgsland = "AT"
                lovvalg = "FOROVRIG"
                grunnlag = "ARBEID"
                medlem = true
                sporingsinformasjon = Sporingsinformasjon().apply {
                    versjon = 0
                    registrert = LocalDate.now()
                    besluttet = LocalDate.now()
                    kilde = "SRVMELOSYS"
                    kildedokument = "DEFAULT_TEST_DOCUMENT"
                    opprettet = LocalDateTime.now()
                    opprettetAv = "SRVMELOSYS"
                    sistEndret = LocalDateTime.now()
                    sistEndretAv = "SRVMELOSYS"
                }
            })
        }
    }

    @Test
    fun testAvsluttBehandlingArt13Jobb() {
        val jobb = AvsluttArt13BehandlingJobb(behandlingService, avsluttArt13BehandlingService)

        val fagsak = Fagsak.forTest {
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
            medBruker()
        }.also {
            addCleanUpAction { slettSakMedAvhengigheter(it.saksnummer) }
            fagsakRepository.save(it)
        }

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

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = MottatteOpplysningerData().apply {
                soeknadsland = Soeknadsland.av(Land_iso2.AT)
            }
            this.behandling = behandling
            this.registrertDato = Instant.now()
            this.endretDato = Instant.now()
            this.type = Mottatteopplysningertyper.SED
            this.versjon = "1.0"
        }

        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)

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
        behandlingsresultat.hentVedtakMetadata().vedtaksdato = Instant.now().minusSeconds(7884000L) // ~3 måneder

        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            Land_iso2.AT,
            null,
            null
        )

        utpekingsperiode.behandlingsresultat = behandlingsresultat
        behandlingsresultat.utpekingsperioder = mutableSetOf(utpekingsperiode)

        Lovvalgsperiode().apply {
            this.behandlingsresultat = behandlingsresultat
            fom = LocalDate.now()
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            dekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
            medlPeriodeID = 1242L
            lovvalgsland = Land_iso2.AT
        }.also {
            lovvalgsperiodeRepository.save(it)
        }

        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)

        jobb.avsluttBehandlingArt13()

        behandlingService.hentBehandling(behandling.id).status.shouldBe(Behandlingsstatus.AVSLUTTET)
    }

}
