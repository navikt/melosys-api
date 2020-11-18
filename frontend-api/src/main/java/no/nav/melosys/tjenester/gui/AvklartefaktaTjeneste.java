package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaStrukturertDto;
import no.nav.melosys.tjenester.gui.dto.VirksomheterDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/avklartefakta")
@Api(tags = { "avklartefakta" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaTjeneste {

    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    private final TilgangService tilgangService;

    @Autowired
    public AvklartefaktaTjeneste(AvklartefaktaService avklartefaktaService, TilgangService tilgangService, AvklarteVirksomheterService avklarteVirksomheterService) {
        this.avklartefaktaService = avklartefaktaService;
        this.tilgangService = tilgangService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling",
                  response = Avklartefakta.class,
                  responseContainer = "Set")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@PathVariable("behandlingID") long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        tilgangService.sjekkTilgang(behandlingID);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre avklartefakta")
    public Set<AvklartefaktaDto> lagreAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                    @RequestBody Set<AvklartefaktaDto> avklartefaktaDtoer) throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @GetMapping("{behandlingID}/strukturert")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling som strukturert objekt", response = AvklartefaktaStrukturertDto.class)
    public AvklartefaktaStrukturertDto hentAvklarteFaktaStrukturert(@PathVariable("behandlingID") long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        tilgangService.sjekkTilgang(behandlingID);

        return AvklartefaktaStrukturertDto.tilAvklartefaktaStrukturertDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/virksomheter")
    @ApiOperation(value = "Lagre virksomheter som avklartefakta", response = AvklartefaktaStrukturertDto.class)
    public AvklartefaktaStrukturertDto lagreVirksomheterSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                   @RequestBody VirksomheterDto virksomheter) throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomheter.getOrgnummer(), behandlingID);

        return AvklartefaktaStrukturertDto.tilAvklartefaktaStrukturertDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }
}
