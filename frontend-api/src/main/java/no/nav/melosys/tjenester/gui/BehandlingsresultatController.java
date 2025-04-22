package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.behandling.AngiBehandlingsresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/behandlinger")
@Tag(name = "behandlingsresultat")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsresultatController {

    private final BehandlingsresultatService behandlingsresultatService;
    private final AngiBehandlingsresultatService angiBehandlingsresultatService;
    private final Aksesskontroll aksesskontroll;

    public BehandlingsresultatController(BehandlingsresultatService behandlingsresultatService, AngiBehandlingsresultatService angiBehandlingsresultatService, Aksesskontroll aksesskontroll) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.angiBehandlingsresultatService = angiBehandlingsresultatService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}/resultat")
    @Operation(summary = "Hent behandlingsresultat knyttet til en behandling")
    public ResponseEntity<BehandlingsresultatDto> hentBehandlingsresultat(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultatMedKontrollresultat(behandlingID);
        return ResponseEntity.ok(BehandlingsresultatDto.av(resultat));
    }

    @Transactional
    @PostMapping("{behandlingID}/resultat/fritekst")
    @Operation(summary = "Oppdater fritekstene begrunnelseFritekst og innledningFritekst i behandlingsresultatet")
    public ResponseEntity<BehandlingsresultatDto> oppdaterFritekster(@PathVariable("behandlingID") long behandlingID,
                                                                     @RequestBody LagreFritekstDto lagreFritekstDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        return ResponseEntity.ok(BehandlingsresultatDto.av(
            behandlingsresultatService.oppdaterFritekster(
                behandlingID,
                lagreFritekstDto.begrunnelseFritekst(),
                lagreFritekstDto.innledningFritekst(),
                lagreFritekstDto.trygdeavgiftFritekst())
        ));
    }

    @Transactional
    @PostMapping("{behandlingID}/resultat/nyvurderingbakgrunn")
    @Operation(summary = "Oppdater nyVurderingBakgrunn i behandlingsresultatet")
    public ResponseEntity<BehandlingsresultatDto> oppdaterNyVurderingBakgrunn(@PathVariable("behandlingID") long behandlingID,
                                                                              @RequestBody LagreNyVurderingBakgrunnDto nyVurderingBakgrunn) {
        aksesskontroll.autoriserSkriv(behandlingID);

        return ResponseEntity.ok(BehandlingsresultatDto.av(
            behandlingsresultatService.oppdaterNyVurderingBakgrunn(behandlingID, nyVurderingBakgrunn.nyVurderingBakgrunn())
        ));
    }

    @PutMapping("{behandlingID}/resultat/utfallregistreringunntak")
    @Operation(summary = "Oppdater utfallRegistreringUnntak i behandlingsresultatet")
    public ResponseEntity<BehandlingsresultatDto> oppdaterUtfallRegistreringUnntak(@PathVariable("behandlingID") long behandlingID,
                                                                                   @RequestBody OppdaterUtfallRegistreringUnntakDto oppdaterUtfallRegistreringUnntakDto) {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID);

        return ResponseEntity.ok(BehandlingsresultatDto.av(
            behandlingsresultatService.oppdaterUtfallRegistreringUnntak(
                behandlingID,
                oppdaterUtfallRegistreringUnntakDto.utfallRegistreringUnntak())
        ));
    }

    @PostMapping("{behandlingID}/resultat/type")
    @Operation(summary = "Angir behandlingsresultattype og avslutter behandling og sak")
    public ResponseEntity<Void> angiBehandlingsresultattype(
        @PathVariable("behandlingID") long behandlingID,
        @RequestBody AngiBehandlingsresultattypeDto angiBehandlingsresultattypeDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        angiBehandlingsresultatService.oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(behandlingID, angiBehandlingsresultattypeDto.type());

        return ResponseEntity.noContent().build();
    }
}
