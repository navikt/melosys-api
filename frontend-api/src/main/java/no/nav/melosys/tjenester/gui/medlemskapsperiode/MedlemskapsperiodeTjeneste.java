package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"medlemskapsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MedlemskapsperiodeTjeneste {

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;
    private final Aksesskontroll aksesskontroll;

    public MedlemskapsperiodeTjeneste(MedlemskapsperiodeService medlemskapsperiodeService,
                                      MedlemAvFolketrygdenService medlemAvFolketrygdenService,
                                      OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService,
                                      Aksesskontroll aksesskontroll) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.opprettMedlemskapsperiodeService = opprettMedlemskapsperiodeService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> hentMedlemskapsperioder(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID)
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<MedlemskapsperiodeDto> opprettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                           @RequestBody MedlemskapsperiodeOppdateringDto medlemskapsperiodeOppdateringDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.opprettMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeOppdateringDto.fomDato(),
                    medlemskapsperiodeOppdateringDto.tomDato(),
                    medlemskapsperiodeOppdateringDto.innvilgelsesResultat(),
                    medlemskapsperiodeOppdateringDto.trygdedekning())
            )
        );
    }

    @PutMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<MedlemskapsperiodeDto> oppdaterMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                            @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID,
                                                                            @RequestBody MedlemskapsperiodeOppdateringDto medlemskapsperiodeOppdateringDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeID,
                    medlemskapsperiodeOppdateringDto.fomDato(),
                    medlemskapsperiodeOppdateringDto.tomDato(),
                    medlemskapsperiodeOppdateringDto.innvilgelsesResultat(),
                    medlemskapsperiodeOppdateringDto.trygdedekning()
                )
            )
        );
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<Void> slettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                        @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID) {
        aksesskontroll.autoriserSkriv(behandlingID);
        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingID, medlemskapsperiodeID);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/behandlinger/medlemskapsperioder/bestemmelser/{behandlingstema}")
    public ResponseEntity<FolketrygdlovenBestemmelserDto> hentBestemmelserMedVilkaar(@PathVariable("behandlingstema") Behandlingstema behandlingstema) {
        var støttede = opprettMedlemskapsperiodeService.hentStøttedeBestemmelserMedVilkår(behandlingstema)
            .entrySet().stream()
            .map(this::tilBestemmelseMedVilkårOgBegrunnelser)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        var ikkeStøttede = opprettMedlemskapsperiodeService.hentIkkeStøttedeBestemmelser(behandlingstema);

        return ResponseEntity.ok(new FolketrygdlovenBestemmelserDto(støttede, ikkeStøttede));
    }

    private BestemmelseMedVilkårOgBegrunnelserDto tilBestemmelseMedVilkårOgBegrunnelser(Map.Entry<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> bestemmelseMedVilkår) {
        return new BestemmelseMedVilkårOgBegrunnelserDto(
            bestemmelseMedVilkår.getKey(),
            bestemmelseMedVilkår.getValue().stream()
                .map(vilkår -> new VilkårOgBegrunnelserDto(vilkår, opprettMedlemskapsperiodeService.hentMuligeBegrunnelser(vilkår)))
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    @GetMapping("/behandlinger/{behandlingID}/medlemskapsperioder/bestemmelser")
    public ResponseEntity<BestemmelseDto> hentBestemmelse(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);

        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingID)
            .map(avFolketrygden -> ResponseEntity.ok(new BestemmelseDto(avFolketrygden.getBestemmelse())))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder/bestemmelser")
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> opprettMedlemskapsperioderFraBestemmelse(@PathVariable("behandlingID") long behandlingID,
                                                                                                      @RequestBody UtledMedlemskapsperiodeDto utledMedlemskapsperiodeDto
    ) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(behandlingID, utledMedlemskapsperiodeDto.getBestemmelse())
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }
}
