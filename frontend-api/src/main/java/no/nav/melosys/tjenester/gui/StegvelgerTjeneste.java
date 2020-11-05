package no.nav.melosys.tjenester.gui;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;

import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import no.nav.security.token.support.core.api.Protected;

@Protected
@RestController
@RequestMapping("/steglvelger")
@Api(tags = { "stegvelger" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class StegvelgerTjeneste {

    @GetMapping("/hentTrygdedekningerForFTRL")
    @ApiOperation(value = "Hent mulige trygdedekninger for folketrygdloven-søknader")
    public ResponseEntity<List<Trygdedekninger>> hentTrygdedekningerForFTRL() {
        return ResponseEntity.ok().body(List.of(HELSEDEL, HELSEDEL_MED_SYKE_OG_FORELDREPENGER, PENSJONSDEL, HELSE_OG_PENSJONSDEL, HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER));
    }
}
