package no.nav.melosys.service.avgift.fakturering

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.melosys.exception.FunksjonellException
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val log = KotlinLogging.logger { }

private const val MAKS_ANTALL_REFERANSER = 1000

@Protected
@RestController
@Tags(
    Tag(name = "fakturaserie"),
    Tag(name = "admin")
)
@RequestMapping("/admin")
class ManglendeInnbetalingAdminController(
    private val manglendeInnbetalingAdminService: ManglendeInnbetalingAdminService
) {

    @PostMapping("/fakturaserier/saker")
    @Operation(
        summary = "Finn sak for mange fakturaseriereferanser i ett kall",
        description = "Én rad per unik referanse, i samme rekkefølge. saksnummer, saksstatus, sakstype, sakstema, " +
            "behandlingstema og behandlingstype (kodeverkets enum-navn) er null når referansen ikke finnes. " +
            "antallSaker > 1 betyr at referansen finnes på flere saker; da hentes feltene fra nyeste " +
            "behandlingsresultat og dets behandling og sak. 1–$MAKS_ANTALL_REFERANSER unike referanser, ellers 400."
    )
    fun hentSakerForFakturaserier(@RequestBody request: FakturaserieReferanserDto): ResponseEntity<FakturaserieSakerDto> {
        val referanser = request.fakturaserieReferanser.distinct()
        if (referanser.isEmpty() || referanser.size > MAKS_ANTALL_REFERANSER) {
            throw FunksjonellException("Må ha 1–$MAKS_ANTALL_REFERANSER unike fakturaserieReferanser, fikk ${referanser.size}")
        }
        if (null in referanser) throw FunksjonellException("fakturaserieReferanser kan ikke inneholde null")
        val saker = manglendeInnbetalingAdminService.hentSaker(referanser.filterNotNull())
        log.info { "Admin: Hentet saker for ${referanser.size} fakturaserieReferanser, ${saker.count { it.saksnummer != null }} funnet" }
        return ResponseEntity.ok(FakturaserieSakerDto(saker))
    }

    @GetMapping("/manglende-innbetaling/prosesser")
    @Operation(
        summary = "Alle prosesser opprettet for meldinger om manglende innbetaling",
        description = "Prosesstypene MANGLENDE_INNBETALING_VARSELBREV og OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING, " +
            "uansett status. fakturaserieReferanse er null for varselbrev, saksnummer er null når prosessen ikke " +
            "har behandling."
    )
    fun hentManglendeInnbetalingProsesser(): ResponseEntity<ManglendeInnbetalingProsesserDto> {
        val prosesser = manglendeInnbetalingAdminService.hentProsesser()
        log.info { "Admin: Hentet ${prosesser.size} prosesser for manglende innbetaling" }
        return ResponseEntity.ok(ManglendeInnbetalingProsesserDto(prosesser))
    }
}

data class FakturaserieReferanserDto(val fakturaserieReferanser: List<String?>)

data class FakturaserieSakerDto(val saker: List<FakturaserieSakDto>)

data class FakturaserieSakDto(
    val fakturaserieReferanse: String,
    val saksnummer: String?,
    val saksstatus: String?,
    val antallSaker: Int,
    val sakstype: String?,
    val sakstema: String?,
    val behandlingstema: String?,
    val behandlingstype: String?
)

data class ManglendeInnbetalingProsesserDto(val prosesser: List<ManglendeInnbetalingProsessDto>)

data class ManglendeInnbetalingProsessDto(
    val fakturanummer: String?,
    val fakturaserieReferanse: String?,
    val saksnummer: String?,
    val prosesstype: String,
    val status: String,
    val registrert: Instant
)
