package no.nav.melosys.service.popp

import mu.KotlinLogging
import no.nav.melosys.integrasjon.popp.PoppHentInntektRequest
import no.nav.melosys.integrasjon.popp.PoppInntektClient
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class PensjonsopptjeningOppslag(
    private val behandlingService: BehandlingService,
    private val årsavregningService: ÅrsavregningService,
    private val persondataService: PersondataService,
    private val poppInntektClient: PoppInntektClient,
) {

    fun hent(behandlingID: Long): Pensjonsopptjening {
        val inntektsÅr = årsavregningService.finnGjeldendeÅrForÅrsavregning(behandlingID)
            ?: error("Fant ingen årsavregning for behandling $behandlingID — kan ikke slå opp pensjonsopptjening")

        val behandling = behandlingService.hentBehandling(behandlingID)
        val fnr = persondataService.hentFolkeregisterident(behandling.fagsak.hentBrukersAktørID())

        val fomAr = inntektsÅr - ANTALL_ÅR_TILBAKE
        val tomAr = inntektsÅr

        log.info {
            "Henter pensjonsopptjening fra POPP for behandling $behandlingID, inntektsÅr=$inntektsÅr, vindu=$fomAr..$tomAr"
        }

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = fnr, fomAr = fomAr, tomAr = tomAr)
        )

        val perioder = respons.inntekter.mapNotNull { post ->
            val aar = post.inntektAr ?: return@mapNotNull null
            val pgi = post.belop ?: return@mapNotNull null
            val kilde = post.kilde ?: UKJENT_KILDE
            PensjonsopptjeningPeriode(aar = aar, pgi = pgi, kilde = kilde, inntektType = post.inntektType)
        }.sortedWith(compareByDescending<PensjonsopptjeningPeriode> { it.aar }.thenBy { it.kilde })

        return Pensjonsopptjening(
            inntektsAr = inntektsÅr,
            behandletAr = inntektsÅr,
            perioder = perioder,
        )
    }

    companion object {
        private const val ANTALL_ÅR_TILBAKE = 4
        private const val UKJENT_KILDE = "UKJENT"
    }
}

data class Pensjonsopptjening(
    val inntektsAr: Int,
    val behandletAr: Int,
    val perioder: List<PensjonsopptjeningPeriode>,
)

data class PensjonsopptjeningPeriode(
    val aar: Int,
    val pgi: Long,
    val kilde: String,
    val inntektType: String? = null,
)
