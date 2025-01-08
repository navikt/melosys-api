package no.nav.melosys.tjenester.gui.avklartefakta;

import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivoppholdtype;
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype;
import no.nav.melosys.service.avklartefakta.*;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto;
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/avklartefakta")
@Api(tags = {"avklartefakta"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaController {

    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarteFaktaArbeidslandService avklarteFaktaArbeidslandService;
    private final AvklartManglendeInnbetalingService avklartManglendeInnbetalingService;
    private final AvklartFamilieRelasjonTypeService avklartFamilieRelasjonTypeService;
    private final AvklartArbeidssituasjonTypeService avklartArbeidssituasjonTypeService;
    private final AvklartOppholdTypeService avklartOppholdTypeService;
    private final AvklartUkjentSluttdatoService avklartUkjentSluttdatoService;

    private final Aksesskontroll aksesskontroll;

    public AvklartefaktaController(AvklartefaktaService avklartefaktaService,
                                   AvklarteVirksomheterService avklarteVirksomheterService,
                                   AvklarteFaktaArbeidslandService avklarteFaktaArbeidslandService,
                                   AvklartArbeidssituasjonTypeService avklartArbeidssituasjonTypeService,
                                   Aksesskontroll aksesskontroll,
                                   AvklartManglendeInnbetalingService avklartManglendeInnbetalingService,
                                   AvklartFamilieRelasjonTypeService avklartFamilieRelasjonTypeService,
                                   AvklartOppholdTypeService avklartOppholdTypeService,
                                   AvklartUkjentSluttdatoService avklartUkjentSluttdatoService) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteFaktaArbeidslandService = avklarteFaktaArbeidslandService;
        this.avklartArbeidssituasjonTypeService = avklartArbeidssituasjonTypeService;
        this.aksesskontroll = aksesskontroll;
        this.avklartManglendeInnbetalingService = avklartManglendeInnbetalingService;
        this.avklartFamilieRelasjonTypeService = avklartFamilieRelasjonTypeService;
        this.avklartOppholdTypeService = avklartOppholdTypeService;
        this.avklartUkjentSluttdatoService = avklartUkjentSluttdatoService;
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

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/virksomheter")
    @ApiOperation(value = "Lagre virksomheter som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreVirksomheterSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                          @RequestBody VirksomheterDto virksomheter) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(behandlingID, virksomheter.getVirksomhetIDer());

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/innbetalingsstatus")
    @ApiOperation(value = "Lagre manglende innbetalingsstatus som avklartfakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreFullstendigManglendeInnbetalingSomAvklartFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                            @RequestBody Boolean fullstendigManglendeInnbetaling) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartManglendeInnbetalingService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(behandlingID,
            fullstendigManglendeInnbetaling);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/arbeidsland")
    @ApiOperation(value = "Lagre arbeidsland som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreArbeidslandSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                         @RequestBody ArbeidslandDto arbeidsland) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklarteFaktaArbeidslandService.lagreArbeidslandSomAvklartefakta(behandlingID, arbeidsland.getArbeidsland());

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @DeleteMapping("{behandlingID}/{avklartefaktatype}")
    @ApiOperation(value = "Slett avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto slettAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                           @PathVariable Avklartefaktatyper avklartefaktatype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartefaktaService.slettAvklarteFakta(behandlingID, avklartefaktatype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/arbeidssituasjontype")
    @ApiOperation(value = "Lagre arbeidssituasjontype som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreArbeidssituasjonTypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                  @RequestBody Arbeidssituasjontype arbeidssituasjontype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartArbeidssituasjonTypeService.lagreArbeidssituasjonTypeSomAvklarteFakta(behandlingID, arbeidssituasjontype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }


    @PostMapping("{behandlingID}/familierelasjonstype")
    @ApiOperation(value = "Lagre familierelasjonstype for ikke yrkesaktive som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreFamilierelasjonstypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                  @RequestBody Ikkeyrkesaktivrelasjontype ikkeyrkesaktivrelasjontype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartFamilieRelasjonTypeService.lagreFamilierelasjonstypeSomAvklarteFakta(behandlingID, ikkeyrkesaktivrelasjontype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/oppholdstype")
    @ApiOperation(value = "Lagre oppholdstype for ikke yrkesaktive som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreOppholdstypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                          @RequestBody Ikkeyrkesaktivoppholdtype ikkeyrkesaktivoppholdtype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartOppholdTypeService.lagreOppholdstypeSomAvklarteFakta(behandlingID, ikkeyrkesaktivoppholdtype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/ukjent-sluttdato")
    @ApiOperation(value = "Lagre ukjent sluttdato som avklartefakta", response = AvklartefaktaOppsummeringDto.class)
    public AvklartefaktaOppsummeringDto lagreUkjentSluttdatoSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                             @RequestBody boolean ukjentSluttdato) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartUkjentSluttdatoService.lagreUkjentSluttdatoSomAvklartefakta(behandlingID, ukjentSluttdato);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

}
