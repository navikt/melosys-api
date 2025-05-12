package no.nav.melosys.tjenester.gui.avklartefakta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.kodeverk.*;
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
@Tag(name = "avklartefakta")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaController {

    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final AvklarteFaktaArbeidslandService avklarteFaktaArbeidslandService;
    private final AvklartManglendeInnbetalingService avklartManglendeInnbetalingService;
    private final AvklartFamilieRelasjonTypeService avklartFamilieRelasjonTypeService;
    private final AvklartArbeidssituasjonTypeService avklartArbeidssituasjonTypeService;
    private final AvklartOppholdTypeService avklartOppholdTypeService;
    private final AvklartUkjentSluttdatoMedlemskapsperiodeService avklartUkjentSluttdatoMedlemskapsperiodeService;

    private final Aksesskontroll aksesskontroll;

    public AvklartefaktaController(AvklartefaktaService avklartefaktaService,
                                   AvklarteVirksomheterService avklarteVirksomheterService,
                                   AvklarteFaktaArbeidslandService avklarteFaktaArbeidslandService,
                                   AvklartArbeidssituasjonTypeService avklartArbeidssituasjonTypeService,
                                   Aksesskontroll aksesskontroll,
                                   AvklartManglendeInnbetalingService avklartManglendeInnbetalingService,
                                   AvklartFamilieRelasjonTypeService avklartFamilieRelasjonTypeService,
                                   AvklartOppholdTypeService avklartOppholdTypeService,
                                   AvklartUkjentSluttdatoMedlemskapsperiodeService avklartUkjentSluttdatoMedlemskapsperiodeService) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.avklarteFaktaArbeidslandService = avklarteFaktaArbeidslandService;
        this.avklartArbeidssituasjonTypeService = avklartArbeidssituasjonTypeService;
        this.aksesskontroll = aksesskontroll;
        this.avklartManglendeInnbetalingService = avklartManglendeInnbetalingService;
        this.avklartFamilieRelasjonTypeService = avklartFamilieRelasjonTypeService;
        this.avklartOppholdTypeService = avklartOppholdTypeService;
        this.avklartUkjentSluttdatoMedlemskapsperiodeService = avklartUkjentSluttdatoMedlemskapsperiodeService;
    }

    @GetMapping("{behandlingID}/oppsummering")
    @Operation(summary = "Henter avklartefakta for en gitt behandling som strukturert objekt")
    public AvklartefaktaOppsummeringDto hentAvklarteFaktaStrukturert(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/virksomheter")
    @Operation(summary = "Lagre virksomheter som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreVirksomheterSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                          @RequestBody VirksomheterDto virksomheter) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(behandlingID, virksomheter.getVirksomhetIDer());

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/innbetalingsstatus")
    @Operation(summary = "Lagre manglende innbetalingsstatus som avklartfakta")
    public AvklartefaktaOppsummeringDto lagreFullstendigManglendeInnbetalingSomAvklartFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                            @RequestBody Boolean fullstendigManglendeInnbetaling) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartManglendeInnbetalingService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(behandlingID,
            fullstendigManglendeInnbetaling);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/arbeidsland")
    @Operation(summary = "Lagre arbeidsland som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreArbeidslandSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                         @RequestBody ArbeidslandDto arbeidsland) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklarteFaktaArbeidslandService.lagreArbeidslandSomAvklartefakta(behandlingID, arbeidsland.getArbeidsland());

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @DeleteMapping("{behandlingID}/{avklartefaktatype}")
    @Operation(summary = "Slett avklartefakta")
    public AvklartefaktaOppsummeringDto slettAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                           @PathVariable Avklartefaktatyper avklartefaktatype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartefaktaService.slettAvklarteFakta(behandlingID, avklartefaktatype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/arbeidssituasjontype")
    @Operation(summary = "Lagre arbeidssituasjontype som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreArbeidssituasjonTypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                  @RequestBody Arbeidssituasjontype arbeidssituasjontype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartArbeidssituasjonTypeService.lagreArbeidssituasjonTypeSomAvklarteFakta(behandlingID, arbeidssituasjontype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }


    @PostMapping("{behandlingID}/familierelasjonstype")
    @Operation(summary = "Lagre familierelasjonstype for ikke yrkesaktive som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreFamilierelasjonstypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                  @RequestBody Ikkeyrkesaktivrelasjontype ikkeyrkesaktivrelasjontype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartFamilieRelasjonTypeService.lagreFamilierelasjonstypeSomAvklarteFakta(behandlingID, ikkeyrkesaktivrelasjontype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/oppholdstype")
    @Operation(summary = "Lagre oppholdstype for ikke yrkesaktive som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreOppholdstypeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                          @RequestBody Ikkeyrkesaktivoppholdtype ikkeyrkesaktivoppholdtype) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartOppholdTypeService.lagreOppholdstypeSomAvklarteFakta(behandlingID, ikkeyrkesaktivoppholdtype);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

    @PostMapping("{behandlingID}/ukjent-sluttdato-medlemskapsperiode")
    @Operation(summary = "Lagre ukjent sluttdato som avklartefakta")
    public AvklartefaktaOppsummeringDto lagreUkjentSluttdatoMedlemskapsperiodeSomAvklarteFakta(@PathVariable("behandlingID") long behandlingID,
                                                                                               @RequestBody boolean ukjentSluttdato) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.AVKLARTE_FAKTA);

        avklartUkjentSluttdatoMedlemskapsperiodeService.lagreUkjentSluttdatoMedlemskapsperiodeSomAvklartefakta(behandlingID, ukjentSluttdato);

        return new AvklartefaktaOppsummeringDto(avklartefaktaService.hentAlleAvklarteFakta(behandlingID));
    }

}
