package no.nav.melosys.melosysmock.medl

import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakSoekRequest
import no.nav.melosys.melosysmock.medl.MedlRepo.repo
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@Unprotected
class MedlApi {

    /**
     * Søker etter medlemskapsperioder basert på søkekriterier
     */
    @PostMapping("/rest/v1/periode/soek")
    fun soekMedlemskapsperioder(
        @RequestBody request: MedlemskapsunntakSoekRequest
    ): List<MedlemskapsunntakForGet> =
        repo.finn(request.personident, request.fraOgMed, request.tilOgMed).toList()

    @GetMapping("/api/v1/medlemskapsunntak/{periodeId}")
    fun hentPeriode(
        @PathVariable periodeId: Long
    ): MedlemskapsunntakForGet =
        repo.hent(periodeId)

    @PostMapping("/api/v1/medlemskapsunntak")
    fun opprettPeriode(
        @RequestBody medlemskapsunntakForPost: MedlemskapsunntakForPost
    ): MedlemskapsunntakForGet =
        repo.opprett(medlemskapsunntakForPost)

    @PutMapping("/api/v1/medlemskapsunntak")
    fun oppdaterPeriode(
        @RequestBody medlemskapsunntakForPut: MedlemskapsunntakForPut
    ): MedlemskapsunntakForGet =
        repo.oppdater(medlemskapsunntakForPut)
}
