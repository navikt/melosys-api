package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.Sporingsinformasjon
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.AnnullerSakService
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Stream

class AnnuleringNyVurderingEøsOgTrygdeavtale(
    @Autowired private val annullerSakService: AnnullerSakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val lovvalgsperiodeRepository: LovvalgsperiodeRepository,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
) : AvgiftFaktureringTestBase() {

    override val fakturaserieReferanse: String = "TestReferanseFakturaserie"

    @ParameterizedTest
    @MethodSource("eøsOgTrygdeavtaleSaker")
    fun `annullering av ny vurdering skal sette saksstatus, avvise medl og kansellere faktura`(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        lovvalgsbestemmelse: LovvalgBestemmelse,
        lovvalgsland: Land_iso2
    ) {
        val saksnummer = "MEL-${UUID.randomUUID().toString().take(8)}"

        val fagsak = Fagsak.forTest {
            this.saksnummer = saksnummer
            type = sakstype
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
            medBruker { aktørId = "1111111111111" }
        }.also {
            addCleanUpAction { slettSakMedAvhengigheter(it.saksnummer) }
            fagsakRepository.save(it)
        }

        val behandlingSomSkalReplikeres = behandlingService.nyBehandling(
            fagsak,
            Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.NY_VURDERING,
            behandlingstema,
            "system",
            "system",
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            "Årsakfritekst"
        )

        val originalBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingSomSkalReplikeres.id)
            .apply {
                fakturaserieReferanse = this@AnnuleringNyVurderingEøsOgTrygdeavtale.fakturaserieReferanse
            }

        Lovvalgsperiode().apply {
            this.behandlingsresultat = originalBehandlingsresultat
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = lovvalgsbestemmelse
            this.lovvalgsland = lovvalgsland
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING
            fom = LocalDate.now().minusMonths(1)
            tom = LocalDate.now().plusMonths(1)
            medlPeriodeID = 78901L
        }.also {
            lovvalgsperiodeRepository.save(it)
        }
        behandlingsresultatRepository.saveAndFlush(originalBehandlingsresultat)

        MedlRepo.repo[78901L] = MedlemskapsunntakForGet().apply {
            unntakId = 78901L
            ident = "30056928150"
            fraOgMed = LocalDate.now().minusMonths(1)
            tilOgMed = LocalDate.now().plusMonths(1)
            status = "GYLD"
            dekning = "Full"
            lovvalg = "ENDL"
            grunnlag = "ARBEID"
            medlem = true
            sporingsinformasjon = Sporingsinformasjon().apply {
                versjon = 0
                registrert = LocalDate.now()
                besluttet = LocalDate.now()
                kilde = "SRVMELOSYS"
                kildedokument = "ANNULERING_TEST"
                opprettet = LocalDateTime.now()
                opprettetAv = "SRVMELOSYS"
                sistEndret = LocalDateTime.now()
                sistEndretAv = "SRVMELOSYS"
            }
        }

        val nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            behandlingSomSkalReplikeres,
            Behandlingstyper.NY_VURDERING
        )

        behandlingsresultatService.hentBehandlingsresultat(nyBehandling.id)
            .apply {
                fakturaserieReferanse = this@AnnuleringNyVurderingEøsOgTrygdeavtale.fakturaserieReferanse
            }
            .also(behandlingsresultatRepository::saveAndFlush)

        val lagretLovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(nyBehandling.id)
        val medlPeriodeId = lagretLovvalgsperiode.medlPeriodeID!!

        executeAndWait(mapOf(ProsessType.ANNULLER_SAK to 1)) {
            annullerSakService.annullerSak(saksnummer)
        }

        fagsakRepository.findById(saksnummer).orElseThrow().apply {
            status shouldBe Saksstatuser.ANNULLERT
        }

        behandlingRepository.findById(nyBehandling.id).orElseThrow().apply {
            status shouldBe Behandlingsstatus.AVSLUTTET
        }

        behandlingsresultatService.hentBehandlingsresultat(nyBehandling.id).apply {
            type shouldBe no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.ANNULLERT
        }

        lovvalgsperiodeRepository.findByBehandlingsresultatId(nyBehandling.id).shouldBeEmpty()

        MedlRepo.repo[medlPeriodeId]!!.apply {
            status shouldBe "AVST"
        }

        mockServer.verify(
            1,
            WireMock.deleteRequestedFor(WireMock.urlEqualTo("/fakturaserier/$fakturaserieReferanse"))
        )
    }

    companion object {
        @JvmStatic
        fun eøsOgTrygdeavtaleSaker(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            arguments(
                Sakstyper.EU_EOS,
                Behandlingstema.ARBEID_FLERE_LAND,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B,
                Land_iso2.BE
            ),
            arguments(
                Sakstyper.TRYGDEAVTALE,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_1,
                Land_iso2.US
            )
        )
    }
}
