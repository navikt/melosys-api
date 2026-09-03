package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.soknad.UlikPeriodeUtleder;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.MottatteOpplysningerGetDto;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.MottatteOpplysningerPostDto;
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.PeriodeOgLandPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Tag(name = "mottatteopplysninger")
@RequestMapping("/mottatteopplysninger")
public class MottatteOpplysningerController {

    private final MottatteOpplysningerService mottatteOpplysningerService;
    private final UlikPeriodeUtleder ulikPeriodeUtleder;
    private final Aksesskontroll aksesskontroll;

    public MottatteOpplysningerController(MottatteOpplysningerService mottatteOpplysningerService,
                                       UlikPeriodeUtleder ulikPeriodeUtleder,
                                       Aksesskontroll aksesskontroll) {
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.ulikPeriodeUtleder = ulikPeriodeUtleder;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity<MottatteOpplysningerGetDto> hentEllerOpprettMottatteOpplysninger(
        @PathVariable(value = "behandlingID") long behandlingID
    ) {
        aksesskontroll.autoriser(behandlingID);

        boolean behandlingKanRedigeresAvSaksbehandler = aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandlingID);
        MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandlingID, behandlingKanRedigeresAvSaksbehandler);
        return ResponseEntity.ok(new MottatteOpplysningerGetDto(mottatteOpplysninger, ulikPeriodeUtleder.harUlikPeriode(mottatteOpplysninger)));
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity<MottatteOpplysningerGetDto> oppdaterMottatteOpplysninger(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody MottatteOpplysningerPostDto mottatteOpplysningerPostDto
    ) {

        aksesskontroll.autoriserSkriv(behandlingID);
        MottatteOpplysninger mottatteOpplysninger = mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandlingID, mottatteOpplysningerPostDto.getData());
        return ResponseEntity.ok(new MottatteOpplysningerGetDto(mottatteOpplysninger, ulikPeriodeUtleder.harUlikPeriode(mottatteOpplysninger)));
    }

    @PostMapping("/{behandlingID}/periodeOgLand")
    public ResponseEntity<Void> oppdaterMottatteOpplysningerPeriodeOgLand(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody PeriodeOgLandPostDto periodeOgLandPostDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingID,
            new Periode(periodeOgLandPostDto.fom(), periodeOgLandPostDto.tom()),
            new Soeknadsland(periodeOgLandPostDto.land(), false));
        return ResponseEntity.noContent().build();
    }
}
