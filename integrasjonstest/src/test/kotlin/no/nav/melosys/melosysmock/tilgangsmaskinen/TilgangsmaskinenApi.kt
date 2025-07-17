package no.nav.melosys.melosysmock.tilgangsmaskinen

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tilgangsmaskinen/api/v1")
open class TilgangsmaskinenApi {

    @PostMapping("/komplett")
    open fun komplettTilgangskontroll(): Boolean {
        return true
    }
}
