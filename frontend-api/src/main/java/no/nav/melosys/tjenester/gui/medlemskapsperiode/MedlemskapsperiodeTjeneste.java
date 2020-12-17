package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService;
import no.nav.melosys.tjenester.gui.dto.FolketrygdlovenbestemmelseMedVilkaarDto;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeOppdatering;
import no.nav.melosys.tjenester.gui.dto.UtledMedlemskapsperiodeDto;
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
    private final OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;
    private final TilgangService tilgangService;

    public MedlemskapsperiodeTjeneste(MedlemskapsperiodeService medlemskapsperiodeService, OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService, TilgangService tilgangService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.opprettMedlemskapsperiodeService = opprettMedlemskapsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> hentMedlemskapsperioder(@PathVariable("behandlingID") long behandlingID) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID)
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<MedlemskapsperiodeDto> opprettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                           @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.opprettMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeOppdatering.getFomDato(),
                    medlemskapsperiodeOppdatering.getTomDato(),
                    medlemskapsperiodeOppdatering.getInnvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.getTrygdedekning())
            )
        );
    }

    @PutMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<MedlemskapsperiodeDto> oppdaterMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                            @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID,
                                                                            @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeID,
                    medlemskapsperiodeOppdatering.getFomDato(),
                    medlemskapsperiodeOppdatering.getTomDato(),
                    medlemskapsperiodeOppdatering.getInnvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.getTrygdedekning()
                )
            )
        );
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<Void> slettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                        @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingID, medlemskapsperiodeID);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/behandlinger/medlemskapsperioder/bestemmelser")
    public ResponseEntity<Collection<FolketrygdlovenbestemmelseMedVilkaarDto>> hentBestemmelserMedVilkaar() {
        return ResponseEntity.ok(
            opprettMedlemskapsperiodeService.hentBestemmelserMedVilkaar()
                .entrySet()
                .stream()
                .map(this::tilBestemmelseMedVilkårDto)
                .collect(Collectors.toSet())
        );
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
    ) throws FunksjonellException, TekniskException {

        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(behandlingID, utledMedlemskapsperiodeDto.getBestemmelse())
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }
}
