package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.AngiBehandlingsresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.AngiBehandlingsresultattypeDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import no.nav.melosys.tjenester.gui.dto.LagreFritekstDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/behandlinger")
@Api(tags = {"behandlingsresultat"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsresultatTjeneste {

    private final BehandlingsresultatService behandlingsresultatService;
    private final AngiBehandlingsresultatService angiBehandlingsresultatService;
    private final Aksesskontroll aksesskontroll;

    public BehandlingsresultatTjeneste(BehandlingsresultatService behandlingsresultatService, AngiBehandlingsresultatService angiBehandlingsresultatService, Aksesskontroll aksesskontroll) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.angiBehandlingsresultatService = angiBehandlingsresultatService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}/resultat")
    @ApiOperation(value = "Hent behandlingsresultat knyttet til en behandling",
        response = BehandlingsresultatDto.class)
    public ResponseEntity<BehandlingsresultatDto> hentBehandlingsresultat(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultatMedKontrollresultat(behandlingID);
        return ResponseEntity.ok(BehandlingsresultatDto.av(resultat));
    }

    @Transactional
    @PostMapping("{behandlingID}/resultat/fritekst")
    @ApiOperation(value = "Oppdater fritekstene begrunnelseFritekst og innledningFritekst i behandlingsresultatet",
        response = BehandlingsresultatDto.class)
    public ResponseEntity<BehandlingsresultatDto> oppdaterFritekster(@PathVariable("behandlingID") long behandlingID,
                                                                     @RequestBody LagreFritekstDto lagreFritekstDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        return ResponseEntity.ok(BehandlingsresultatDto.av(
            behandlingsresultatService.oppdaterFritekster(
                behandlingID,
                lagreFritekstDto.begrunnelseFritekst(),
                lagreFritekstDto.innledningFritekst())
        ));
    }

    @PostMapping("{behandlingID}/resultat/type")
    @ApiOperation(value = "Angir behandlingsresultattype og avslutter behandling og sak")
    public ResponseEntity<Void> angiBehandlingsresultattype(
            @PathVariable("behandlingID") long behandlingID,
            @RequestBody AngiBehandlingsresultattypeDto angiBehandlingsresultattypeDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        var behandlingsresultattype = Behandlingsresultattyper.valueOf(angiBehandlingsresultattypeDto.type());
        angiBehandlingsresultatService.oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(behandlingID, behandlingsresultattype);

        return ResponseEntity.noContent().build();
    }
}
