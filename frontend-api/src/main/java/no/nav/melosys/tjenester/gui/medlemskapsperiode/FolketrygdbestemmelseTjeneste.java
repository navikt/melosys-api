package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import java.util.Collection;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.tjenester.gui.dto.FolketrygdlovenbestemmelseMedVilkaarDto;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@Api(tags = {"medlemskapsperioder"})
@RestController
public class FolketrygdbestemmelseTjeneste {

    private final MedlemskapsperiodeService medlemskapsperiodeService;

    public FolketrygdbestemmelseTjeneste(MedlemskapsperiodeService medlemskapsperiodeService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
    }

    @GetMapping("/behandlinger/medlemskapsperioder/bestemmelser")
    public ResponseEntity<Collection<FolketrygdlovenbestemmelseMedVilkaarDto>> hentBestemmelserMedVilkaar() {
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentBestemmelserMedVilkaar()
                .entrySet()
                .stream()
                .map(entry -> new FolketrygdlovenbestemmelseMedVilkaarDto(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet())
        );
    }
}
