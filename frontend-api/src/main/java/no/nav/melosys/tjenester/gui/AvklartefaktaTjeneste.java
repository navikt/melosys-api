package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.VirksomheterDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/avklartefakta")
@Api(tags = {"avklartefakta"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaTjeneste {

    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final Aksesskontroll aksesskontroll;

    public AvklartefaktaTjeneste(AvklartefaktaService avklartefaktaService,
                                 AvklarteVirksomheterService avklarteVirksomheterService,
                                 AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                 Aksesskontroll aksesskontroll) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling",
        response = Avklartefakta.class,
        responseContainer = "Set")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre avklartefakta")
    public Set<AvklartefaktaDto> lagreAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                    @RequestBody Set<AvklartefaktaDto> avklartefaktaDtoer) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @GetMapping("{behandlingID}/oppsummering")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling som strukturert objekt", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto hentAvklarteFaktaStrukturert(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return AvklartefaktaOppsummeringDto.av(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/virksomheter")
    @ApiOperation(value = "Lagre virksomheter som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreVirksomheterSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                          @RequestBody VirksomheterDto virksomheter) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(behandlingID, virksomheter.getVirksomhetIDer());

        return AvklartefaktaOppsummeringDto.av(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }
}
