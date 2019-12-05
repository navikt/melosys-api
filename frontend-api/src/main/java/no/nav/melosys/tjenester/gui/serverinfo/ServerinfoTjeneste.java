package no.nav.melosys.tjenester.gui.serverinfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Unprotected
@Api(tags = {"serverinfo"})
@RestController("/serverinfo")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ServerinfoTjeneste {
    @GetMapping
    @ApiOperation(
        value = "Henter informasjon om miljø og bygg av backend.",
        response = ServerinfoDto.class
    )
    public ResponseEntity hentServerStatus() {
        return ResponseEntity.ok(Serverinfo.tilDto());
    }
}
