package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.VilkaarSpørsmål;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kodeverk/melosys-internt")
@Api(tags = { "kodeverk/melosys-internt"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MelosysInterntKodeverkTjeneste {

    private final MedlemskapsperiodeService medlemskapsperiodeService;

    public MelosysInterntKodeverkTjeneste(MedlemskapsperiodeService medlemskapsperiodeService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
    }

    @GetMapping("/folketrygden")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    public ResponseEntity<Map<String, Collection<KodeDto>>> hentKoderTilFolketrygden() {
        Map<String, Collection<KodeDto>> kodeverdier = new HashMap<>();
        kodeverdier.put(Trygdedekninger.class.getSimpleName(), tilKodeDto(medlemskapsperiodeService.hentGyldigeTrygdedekninger()));
        kodeverdier.put(VilkaarSpørsmål.class.getSimpleName(), tilKodeDto(VilkaarSpørsmål.values()));
        return ResponseEntity.ok(kodeverdier);
    }

    private <T extends Kodeverk> Collection<KodeDto> tilKodeDto(Collection<T> kodeverk) {
        return tilKodeDto(kodeverk.toArray(kodeverk.toArray(new Kodeverk[0])));
    }

    private Collection<KodeDto> tilKodeDto(Kodeverk... kodeverk) {
        return Stream.of(kodeverk).map(k -> new KodeDto(k.getKode(), k.getBeskrivelse())).collect(Collectors.toSet());
    }


}
