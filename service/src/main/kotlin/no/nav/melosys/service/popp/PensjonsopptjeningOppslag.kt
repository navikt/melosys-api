package no.nav.melosys.service.popp

import mu.KotlinLogging
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.popp.PoppHentInntektRequest
import no.nav.melosys.integrasjon.popp.PoppInntektClient
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import java.time.LocalDate

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
            ?: throw IkkeFunnetException("Fant ingen årsavregning for behandling $behandlingID")

        val behandling = behandlingService.hentBehandling(behandlingID)
        val fnr = persondataService.hentFolkeregisterident(behandling.fagsak.hentBrukersAktørID())

        val fomAr = inntektsÅr - ANTALL_ÅR_TILBAKE
        val tomAr = inntektsÅr

        log.info {
            "Henter pensjonsopptjening fra POPP for behandling $behandlingID, inntektsÅr=$inntektsÅr, vindu=$fomAr..$tomAr"
        }

        val respons = poppInntektClient.hentInntekt(
            PoppHentInntektRequest(fnr = fnr, fomAr = fomAr, tomAr = tomAr, inntektType = SUM_PI)
        )

        val perioder = (respons.inntekter ?: emptyList()).mapNotNull { post ->
            val aar = post.inntektAr ?: return@mapNotNull null
            val pgi = post.belop ?: return@mapNotNull null
            if (post.kilde == null) {
                log.warn { "POPP-post uten kilde for behandling $behandlingID, år=$aar — markeres som UKJENT" }
            }
            val kilde = post.kilde ?: UKJENT_KILDE
            PensjonsopptjeningPeriode(
                aar = aar,
                pgi = pgi,
                kilde = kilde,
                registrert = post.changeStamp?.createdDate.toOsloLocalDate(),
                oppdatert = post.changeStamp?.updatedDate.toOsloLocalDate(),
            )
        }.sortedWith(
            compareByDescending<PensjonsopptjeningPeriode> { it.aar }
                .thenBy { kildePrioritet(it.kilde) }
                .thenBy { it.kilde }
        )

        return Pensjonsopptjening(inntektsAr = inntektsÅr, perioder = perioder)
    }

    private fun kildePrioritet(kilde: String): Int = when (kilde) {
        "SKATT" -> 0
        "MELOSYS" -> 1
        "AVGIFTSSYSTEMET" -> 2
        else -> 99
    }

    companion object {
        private const val ANTALL_ÅR_TILBAKE = 4
        private const val UKJENT_KILDE = "UKJENT"
        private const val SUM_PI = "SUM_PI"
    }
}

data class Pensjonsopptjening(
    val inntektsAr: Int,
    val perioder: List<PensjonsopptjeningPeriode>,
)

data class PensjonsopptjeningPeriode(
    val aar: Int,
    val pgi: Long,
    val kilde: String,
    val registrert: LocalDate? = null,
    val oppdatert: LocalDate? = null,
)
