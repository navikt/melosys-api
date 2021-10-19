package no.nav.melosys.tjenester.gui;

import java.util.*;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeAvtaleDataForVedtakDto;
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
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public TrygdeavtaleTjeneste(TrygdeavtaleService trygdeavtaleService,
                                BehandlingService behandlingService,
                                Aksesskontroll aksesskontroll,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                AvklarteVirksomheterService avklarteVirksomheterService,
                                LovvalgsperiodeService lovvalgsperiodeService) {
        this.trygdeavtaleService = trygdeavtaleService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @GetMapping("{behandlingID}")
    public ResponseEntity<TrygdeavtaleInfoDto> hentTrygdeavtaleInfo(@PathVariable("behandlingID") long behandlingId,
                                                                    @RequestParam(value = "virksomheter", required = false) boolean hentVirksomheter,
                                                                    @RequestParam(value = "barnEktefeller", required = false) boolean hentBarnEktefeller) {

        aksesskontroll.autoriserSkriv(behandlingId);
        var behandling = behandlingService.hentBehandling(behandlingId);
        return ResponseEntity.ok(new TrygdeavtaleInfoDto(
            behandling.getFagsak().hentAktørID(),
            behandling.getTema().getKode(),
            hentVirksomheter ? trygdeavtaleService.hentVirksomheter(behandling) : Collections.emptyMap(),
            hentBarnEktefeller ? trygdeavtaleService.hentFamiliemedlemmer(behandling) : Collections.emptyList()
        ));
    }

    @PostMapping("{behandlingID}")
    public void overforDataForVedtak(
        @PathVariable("behandlingID") long behandlingsId,
        @RequestBody TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {

        // TODO: Flytt dette ut til en egen klasse
        sjekkBehandlingsgrunnlag(behandlingsId, trygdeAvtaleDataForVedtakDto);

        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = lagMedfolgendeFamilieDto(trygdeAvtaleDataForVedtakDto);
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(behandlingsId, lagreMedfolgendeFamilieDto.til());

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(trygdeAvtaleDataForVedtakDto.virksomheter(), behandlingsId);

        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(trygdeAvtaleDataForVedtakDto);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsId, List.of(lovvalgsperiode));
    }

    private LagreMedfolgendeFamilieDto lagMedfolgendeFamilieDto(TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {
        var familie = new ArrayList<>(trygdeAvtaleDataForVedtakDto.barn());
        familie.add(trygdeAvtaleDataForVedtakDto.ektefelle());
        return new LagreMedfolgendeFamilieDto(Set.copyOf(familie));
    }

    private Lovvalgsperiode lagLovvalgsperiode(TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(trygdeAvtaleDataForVedtakDto.fom());
        lovvalgsperiode.setTom(trygdeAvtaleDataForVedtakDto.tom());
        Landkoder lovvalgsland = Landkoder.valueOf(trygdeAvtaleDataForVedtakDto.land()
            .stream().findFirst().orElseThrow(() -> new TekniskException("trygdeAvtaleDataForVedtakDto.land inneholder ingen land")));
        lovvalgsperiode.setLovvalgsland(lovvalgsland);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.valueOf(trygdeAvtaleDataForVedtakDto.bestemmelse()));
        return lovvalgsperiode;
    }

    private void sjekkBehandlingsgrunnlag(long behandlingsId, TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto) {
        // Periode og land skal alltid være det sammne - men er en sanity sjekk for nå
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingsId);
        SoeknadFtrl behandlingsgrunnlagdata = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        Periode periode = behandlingsgrunnlagdata.periode;
        assert periode.getFom().equals(trygdeAvtaleDataForVedtakDto.fom());
        assert periode.getTom().equals(trygdeAvtaleDataForVedtakDto.tom());

        for (var land : trygdeAvtaleDataForVedtakDto.land()) {
            if (!behandlingsgrunnlagdata.soeknadsland.landkoder.contains(land))
                throw new TekniskException("Forventet " + land + " i behandlingsgrunnlagdata soeknadsland.landkoder");
        }
    }
}
