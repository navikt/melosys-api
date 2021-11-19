package no.nav.melosys.tjenester.gui;

import java.util.Collections;

import io.swagger.annotations.Api;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/trygdeavtale")
@Api(tags = {"trygdeavtale"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TrygdeavtaleTjeneste {

    private final TrygdeavtaleService trygdeavtaleService;
    private final BehandlingService behandlingService;
    private final Aksesskontroll aksesskontroll;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                Aksesskontroll aksesskontroll) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}")
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleInfo(@PathVariable("behandlingID") long behandlingId,
                                                                    @RequestParam(value = "virksomheter", required = false) boolean hentVirksomheter,
                                                                    @RequestParam(value = "barnEktefeller", required = false) boolean hentBarnEktefeller) {

        aksesskontroll.autoriserSkriv(behandlingId);
        var behandling = behandlingService.hentBehandling(behandlingId);
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        return ResponseEntity.ok(new TrygdeavtaleInfoDto(
            behandling.getFagsak().hentAktørID(),
            behandling.getTema().getKode(),
            behandlingsgrunnlagdata.periode,
            behandlingsgrunnlagdata.soeknadsland.landkoder,
            hentVirksomheter ? trygdeavtaleService.hentVirksomheter(behandling) : Collections.emptyMap(),
            hentBarnEktefeller ? trygdeavtaleService.hentFamiliemedlemmer(behandling) : Collections.emptyList()
        ));
    }

    @PostMapping("{behandlingID}")
    public ResponseEntity<Void> overførResultat(@PathVariable("behandlingID") long behandlingId,
                                                @RequestBody TrygdeavtaleResultatDto trygdeavtaleResultatDto) {
        aksesskontroll.autoriserSkriv(behandlingId);
        trygdeavtaleService.overførResultat(behandlingId, trygdeavtaleResultatDto.til());

        return ResponseEntity.ok().build();
    }
}
