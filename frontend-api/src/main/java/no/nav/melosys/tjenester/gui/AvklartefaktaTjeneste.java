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
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;
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
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    private final TilgangService tilgangService;

    @Autowired
    public AvklartefaktaTjeneste(AvklartefaktaService avklartefaktaService, TilgangService tilgangService, AvklarteVirksomheterService avklarteVirksomheterService, AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService) {
        this.avklartefaktaService = avklartefaktaService;
        this.tilgangService = tilgangService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
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

    @GetMapping("{behandlingID}/oppsummering")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling som strukturert objekt", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto hentAvklarteFaktaStrukturert(@PathVariable("behandlingID") long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        tilgangService.sjekkTilgang(behandlingID);

        return AvklartefaktaOppsummeringDto.av(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/virksomheter")
    @ApiOperation(value = "Lagre virksomheter som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreVirksomheterSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                   @RequestBody VirksomheterDto virksomheter) throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomheter.getVirksomhetIDer(), behandlingID);

        return AvklartefaktaOppsummeringDto.av(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/medfolgendeFamilie")
    @ApiOperation(value = "Lagre medfolgendeFamilie som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreMedfolgendeFamilieSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
        @RequestBody MedfolgendeFamilieDto medfolgendeFamilieDto) throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingID, medfolgendeFamilieDto.getAvklarteMedfolgendeBarn(), medfolgendeFamilieDto.getAvklarteMedfolgendeEktefelleSamboer());

        return AvklartefaktaOppsummeringDto.av(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }
}
