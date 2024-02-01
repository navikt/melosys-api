package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.MottatteOpplysningerGetDto;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.MottatteOpplysningerPostDto;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.PeriodeOgLandPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = "mottatteopplysninger")
@RequestMapping("/mottatteopplysninger")
public class MottatteOpplysningerTjeneste {

    private final MottatteOpplysningerService mottatteOpplysningerService;
    private final Aksesskontroll aksesskontroll;

    public MottatteOpplysningerTjeneste(MottatteOpplysningerService mottatteOpplysningerService,
                                       Aksesskontroll aksesskontroll) {
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity<MottatteOpplysningerGetDto> hentEllerOpprettMottatteOpplysninger(
        @PathVariable(value = "behandlingID") long behandlingID
    ) {
        aksesskontroll.autoriser(behandlingID);

        boolean behandlingKanRedigeresAvSaksbehandler = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandlingID);
        MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandlingID, behandlingKanRedigeresAvSaksbehandler);
        return ResponseEntity.ok(new MottatteOpplysningerGetDto(mottatteOpplysninger));
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity<MottatteOpplysningerGetDto> oppdaterMottatteOpplysninger(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody MottatteOpplysningerPostDto mottatteOpplysningerPostDto
    ) {

        aksesskontroll.autoriserSkriv(behandlingID);
        MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandlingID, mottatteOpplysningerPostDto.data);
        return ResponseEntity.ok(new MottatteOpplysningerGetDto(mottatteOpplysninger));
    }

    @PostMapping("/{behandlingID}/periodeOgLand")
    public ResponseEntity<Void> oppdaterMottatteOpplysningerPeriodeOgLand(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody PeriodeOgLandPostDto periodeOgLandPostDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingID,
            new Periode(periodeOgLandPostDto.fom, periodeOgLandPostDto.tom),
            new Soeknadsland(periodeOgLandPostDto.land, false));
        return ResponseEntity.noContent().build();
    }
}
