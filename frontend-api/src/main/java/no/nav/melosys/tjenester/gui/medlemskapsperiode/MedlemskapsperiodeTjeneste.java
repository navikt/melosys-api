package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Protected
@RestController
@Api(tags = {"medlemskapsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MedlemskapsperiodeTjeneste {

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;
    private final Aksesskontroll aksesskontroll;

    public MedlemskapsperiodeTjeneste(MedlemskapsperiodeService medlemskapsperiodeService, OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService, Aksesskontroll aksesskontroll) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
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
                                                                           @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.opprettMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeOppdatering.fomDato(),
                    medlemskapsperiodeOppdatering.tomDato(),
                    medlemskapsperiodeOppdatering.innvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.trygdedekning())
            )
        );
    }

    @PutMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<MedlemskapsperiodeDto> oppdaterMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                            @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID,
                                                                            @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeID,
                    medlemskapsperiodeOppdatering.fomDato(),
                    medlemskapsperiodeOppdatering.tomDato(),
                    medlemskapsperiodeOppdatering.innvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.trygdedekning()
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
        var støttede = tilCollectionAvBestemmelseMedVilkår(opprettMedlemskapsperiodeService.hentStøttedeBestemmelserMedVilkaar(behandlingstema));
        var ikkeStøttede = tilCollectionAvBestemmelseMedVilkår(opprettMedlemskapsperiodeService.hentIkkeStøttedeBestemmelserMedVilkaar(behandlingstema));

        return ResponseEntity.ok(new FolketrygdlovenBestemmelserDto(støttede, ikkeStøttede));
    }

    private Collection<FolketrygdlovenbestemmelseMedVilkaarDto> tilCollectionAvBestemmelseMedVilkår(Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> bestemmelseMedVilkår) {
        return bestemmelseMedVilkår
            .entrySet()
            .stream()
            .map(this::tilBestemmelseMedVilkårDto)
            .collect(Collectors.toSet());
    }

    private FolketrygdlovenbestemmelseMedVilkaarDto tilBestemmelseMedVilkårDto(Map.Entry<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> bestemmelseMedVilkår) {
        return new FolketrygdlovenbestemmelseMedVilkaarDto(
            bestemmelseMedVilkår.getKey(),
            bestemmelseMedVilkår.getValue().stream()
                .map(vilkår -> new FolketrygdlovenbestemmelseMedVilkaarDto.VilkårOgBegrunnelse(
                    vilkår,
                    opprettMedlemskapsperiodeService.hentMuligeBegrunnelser(vilkår))
                ).collect(Collectors.toSet()
                ));
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
