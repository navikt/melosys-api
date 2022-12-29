package no.nav.melosys.melosysmock.utbetal

import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import no.nav.melosys.integrasjon.utbetaling.Utbetaling
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")
@Unprotected
class UtbetalDataRestApi {
    @PostMapping
    fun hentPeriodeliste(
        @RequestBody utbetalingRequest: UtbetalingRequest
    ): List<Utbetaling> =
        UtbetaldataRepo.finnUtbetalinger(utbetalingRequest)
}
