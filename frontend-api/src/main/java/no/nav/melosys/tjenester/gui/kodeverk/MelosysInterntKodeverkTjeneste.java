package no.nav.melosys.tjenester.gui.kodeverk;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.tjenester.gui.dto.FolketrygdenKoderDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kodeverk/melosys-internt")
@Api(tags = { "kodeverk/melosys-internt"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MelosysInterntKodeverkTjeneste {

    private static final Logger log = LoggerFactory.getLogger(MelosysInterntKodeverkTjeneste.class);

    private final MedlemskapsperiodeService medlemskapsperiodeService;

    public MelosysInterntKodeverkTjeneste(MedlemskapsperiodeService medlemskapsperiodeService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
    }

    @GetMapping("/folketrygden")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    public ResponseEntity<FolketrygdenKoderDto> hentKoderTilFolketrygden() {
        log.info("Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker.");
        return ResponseEntity.ok(new FolketrygdenKoderDto(medlemskapsperiodeService.hentGyldigeTrygdedekninger()));
    }
}
