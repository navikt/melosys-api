package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.saksflyt.AvslagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.AvslagDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/avslag")
@Api(tags = {"saksflyt", "avslag"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvslagTjeneste {

    private final Aksesskontroll aksesskontroll;
    private final AvslagService avslagService;

    public AvslagTjeneste(Aksesskontroll aksesskontroll, AvslagService avslagService) {
        this.aksesskontroll = aksesskontroll;
        this.avslagService = avslagService;
    }

    @PostMapping("{behandlingID}/manglende-opplysninger")
    @ApiOperation(value = "Avslår behandling pga manglende opplysninger")
    public ResponseEntity<Void> avslåPgaManglendeOpplysninger(@PathVariable("behandlingID") long behandlingID,
                                                              @RequestBody AvslagDto avslagDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        avslagService.avslåPgaManglendeOpplysninger(behandlingID, avslagDto.getFritekst(), SubjectHandler.getInstance().getUserID());
        return ResponseEntity.noContent().build();
    }
}
