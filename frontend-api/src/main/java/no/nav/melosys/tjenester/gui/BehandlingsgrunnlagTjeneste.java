package no.nav.melosys.tjenester.gui;

import java.util.HashSet;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsgrunnlagTilleggsData;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Api(tags = "behandlingsgrunnlag")
@RequestMapping("/behandlingsgrunnlag")
public class BehandlingsgrunnlagTjeneste {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final RegisterOppslagService registerOppslagService;
    private final TilgangService tilgangService;

    public BehandlingsgrunnlagTjeneste(BehandlingsgrunnlagService behandlingsgrunnlagService, RegisterOppslagService registerOppslagService, TilgangService tilgangService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.registerOppslagService = registerOppslagService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity hentBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag, hentTilleggsData(behandlingsgrunnlag)));
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity oppdaterBehandlingsgrunnlag(@PathVariable(value = "behandlingID") long behandlingID,
                                                      @RequestBody BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagPostDto.getData());
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag, hentTilleggsData(behandlingsgrunnlag)));
    }

    private BehandlingsgrunnlagTilleggsData hentTilleggsData(Behandlingsgrunnlag behandlingsgrunnlag) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        if (behandlingsgrunnlag.getType() == BehandlingsGrunnlagType.SED && behandlingsgrunnlag.getBehandlingsgrunnlagdata() instanceof SedGrunnlag) {
            SedGrunnlag sedGrunnlag = (SedGrunnlag) behandlingsgrunnlag.getBehandlingsgrunnlagdata();
            if (!sedGrunnlag.norskeArbeidsgivere.isEmpty()) {
                return new BehandlingsgrunnlagTilleggsData(new HashSet<>(sedGrunnlag.norskeArbeidsgivere));
            }
        }
        return new BehandlingsgrunnlagTilleggsData(
            registerOppslagService.hentOrganisasjoner(behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentAlleOrganisasjonsnumre())
        );
    }
}
