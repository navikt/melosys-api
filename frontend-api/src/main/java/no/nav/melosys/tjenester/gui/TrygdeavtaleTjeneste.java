package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.OrgIdNavnDto;
import no.nav.melosys.tjenester.gui.dto.TrygdeavtaleInfoDto;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@RestController
@RequestMapping("/trygdeavtale")
@Api(tags = {"trygdeavtale"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TrygdeavtaleTjeneste {

    private final TrygdeavtaleService trygdeavtaleService;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService) {
        this.trygdeavtaleService = trygdeavtaleService;
    }

    @Unprotected
    @GetMapping
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleInfo(@RequestParam long behandlingId) {
        return ResponseEntity.ok(new TrygdeavtaleInfoDto(
            OrgIdNavnDto.av(trygdeavtaleService.hentVirksomheter(behandlingId)), null,null));
    }
}
