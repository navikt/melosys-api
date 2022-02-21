package no.nav.melosys.tjenester.gui;

import java.util.Collections;

import io.swagger.annotations.Api;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/trygdeavtale")
@Api(tags = {"trygdeavtale"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TrygdeavtaleTjeneste {

    private static final Logger log = LoggerFactory.getLogger(TrygdeavtaleTjeneste.class);

    private final TrygdeavtaleService trygdeavtaleService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final Aksesskontroll aksesskontroll;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                BehandlingsresultatService behandlingsresultatService,
                                Aksesskontroll aksesskontroll) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.aksesskontroll = aksesskontroll;
    }

    /***
     *
     * @deprecated
     * Dette endepunket blir fjernet når trygdeavtale har tatt i bruk behandlingsgrunnlag/{behandlingID}
     */
    @GetMapping("{behandlingID}")
    @Transactional
    @Deprecated()
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleInfo(@PathVariable("behandlingID") long behandlingId,
                                                                    @RequestParam(value = "virksomheter", required = false) boolean hentVirksomheter,
                                                                    @RequestParam(value = "barnEktefeller", required = false) boolean hentBarnEktefeller) {
        return hentTrygdeavtaleBehandlingsgrunnlag(behandlingId, hentVirksomheter, hentBarnEktefeller);
    }

    @GetMapping("behandlingsgrunnlag/{behandlingID}")
    @Transactional
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleBehandlingsgrunnlag(@PathVariable("behandlingID") long behandlingId,
                                                                                   @RequestParam(value = "virksomheter", required = false) boolean hentVirksomheter,
                                                                                   @RequestParam(value = "barnEktefeller", required = false) boolean hentBarnEktefeller) {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        log.debug("Melosys-trygdeavtale henter TrygdeavtaleInfo for behandling {} på vegne av saksbehandler {}.", behandlingId, saksbehandler);
        aksesskontroll.autoriser(behandlingId);

        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var behandlingsResultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);

        return ResponseEntity.ok(new TrygdeavtaleInfoDto(
            behandling.getFagsak().hentAktørID(),
            behandling.getTema().getKode(),
            behandling.getType().getKode(),
            aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler),
            behandlingsgrunnlagdata.periode,
            behandlingsgrunnlagdata.soeknadsland.landkoder,
            hentVirksomheter ? trygdeavtaleService.hentVirksomheter(behandling) : Collections.emptyMap(),
            hentBarnEktefeller ? trygdeavtaleService.hentFamiliemedlemmer(behandling) : Collections.emptyList(),
            behandlingsResultat.getInnledningFritekst(),
            behandlingsResultat.getBegrunnelseFritekst(),
            behandlingsResultat.getVedtakMetadata() != null ? behandlingsResultat.getVedtakMetadata().getNyVurderingBakgrunn(): null
        ));
    }

    @GetMapping("resultat/{behandlingID}")
    @Transactional
    public ResponseEntity<TrygdeavtaleResultatDto> hentResultat(@PathVariable("behandlingID") long behandlingId) {
        TrygdeavtaleResultat trygdeavtaleResultat = trygdeavtaleService.hentResultat(behandlingId);
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        return ResponseEntity.ok(TrygdeavtaleResultatDto.fra(trygdeavtaleResultat, behandlingsgrunnlagdata.personOpplysninger.medfolgendeFamilie));
    }

    @PostMapping("resultat/{behandlingID}")
    public ResponseEntity<Void> overførTrygdeavtaleResultat(@PathVariable("behandlingID") long behandlingId,
                                                @RequestBody TrygdeavtaleResultatDto trygdeavtaleResultatDto) {
        aksesskontroll.autoriserSkriv(behandlingId);
        trygdeavtaleService.overførResultat(behandlingId, trygdeavtaleResultatDto.til());

        return ResponseEntity.noContent().build();
    }

    /***
     *
     * @deprecated
     * Dette endepunket blir fjernet når trygdeavtale har tatt i bruk resultat/{behandlingID}
     */
    @PostMapping("{behandlingID}")
    public ResponseEntity<Void> overførResultat(@PathVariable("behandlingID") long behandlingId,
                                                @RequestBody TrygdeavtaleResultatDto trygdeavtaleResultatDto) {
        return overførTrygdeavtaleResultat(behandlingId, trygdeavtaleResultatDto);
    }
}
