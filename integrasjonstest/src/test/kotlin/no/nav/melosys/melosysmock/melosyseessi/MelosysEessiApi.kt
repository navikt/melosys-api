package no.nav.melosys.melosysmock.melosyseessi

import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedInformasjon
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*
import java.time.ZoneOffset

@RestController
@RequestMapping("/api")
@Unprotected
class MelosysEessiApi {

    companion object SaksrelasjonLager {
        val saksrelasjoner = mutableSetOf<Saksrelasjon>()
        val bucInformasjoner = mutableSetOf<BucInformasjon>()
    }

    @GetMapping("/buc/{bucType}/institusjoner")
    fun hentMottakerinstitusjoner() = emptyArray<Unit>()

    @GetMapping("/sak")
    fun hentSaksrelasjon(@RequestParam("rinaSaksnummer") rinaSaksnummer: String) =
        saksrelasjoner.filter { s -> s.rinaSaksnummer == rinaSaksnummer }

    @PostMapping("/sak")
    fun lagreSaksrelasjon(@RequestBody saksrelasjon: Saksrelasjon) {
        saksrelasjoner.add(saksrelasjon)
    }

    @GetMapping("/sak/{arkivSakID}/bucer")
    fun hentTilknyttedeBucer(
        @PathVariable("arkivSakID") arkivSakId: String,
        @RequestParam(required = false) statuser: List<String>
    ): List<BucinfoDto> {
        val map = saksrelasjoner.filter { s -> s.gsakSaksnummer == arkivSakId.toLong() }.map { p -> p.rinaSaksnummer }
        return MelosysEessiRepo.repo.filter { s -> s.id in map }
            .map { b ->
                b.toDomain()
            }
    }
}

data class Saksrelasjon(
    val gsakSaksnummer: Long? = null,
    val rinaSaksnummer: String? = null,
    val bucType: String? = null
)

data class BucinfoDto(
    var id: String? = null,
    val erÅpen: Boolean = false,
    val bucType: String? = null,
    val opprettetDato: Long? = null,
    val mottakerinstitusjoner: Set<String>? = null,
    val seder: List<SedinfoDto>? = null
)

fun BucInformasjon.toDomain() = BucinfoDto(
    id = this.id,
    erÅpen = this.erÅpen(),
    bucType = this.bucType,
    opprettetDato = this.opprettetDato.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
    mottakerinstitusjoner = this.mottakerinstitusjoner,
    seder = this.seder.map { s -> s.toDto() }
)

data class SedinfoDto(
    val bucId: String? = null,
    val sedId: String? = null,
    val opprettetDato: Long? = null,
    val sistOppdatert: Long? = null,
    val sedType: String? = null,
    val status: String? = null,
    val rinaUrl: String? = null,
)

fun SedInformasjon.toDto() = SedinfoDto(
    bucId = this.bucId,
    sedId = this.sedId,
    opprettetDato = this.opprettetDato.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
    sistOppdatert = this.sistOppdatert.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
    sedType = this.sedType,
    status = this.status,
    rinaUrl = this.rinaUrl
)
