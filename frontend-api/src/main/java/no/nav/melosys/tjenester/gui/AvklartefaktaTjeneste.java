package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@Api(tags = { "avklartefakta" })
@RestController("/avklartefakta")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaTjeneste extends RestTjeneste {

    private AvklartefaktaService avklartefaktaService;

    private final TilgangService tilgangService;

    @Autowired
    public AvklartefaktaTjeneste(AvklartefaktaService avklartefaktaService, TilgangService tilgangService) {
        this.avklartefaktaService = avklartefaktaService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling",
                  response = Avklartefakta.class,
                  responseContainer = "Set")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@ApiParam("BehandlingsID") @PathVariable("behandlingID") long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        tilgangService.sjekkTilgang(behandlingID);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre avklartefakta")
    public Set<AvklartefaktaDto> lagreAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                    @ApiParam("AvklartefaktaData") Set<AvklartefaktaDto> avklartefaktaDtoer) throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }
}
