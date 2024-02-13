package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.*;
import java.util.stream.Stream;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.*;
import no.nav.melosys.service.kodeverk.KodeDto;
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
@Api(tags = {"kodeverk/melosys-internt"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MelosysInterntKodeverkTjeneste {

    private static final List<InnvilgelsesResultat> gyldigeInnvilgelsesResultat = Arrays.stream(InnvilgelsesResultat.values())
        .filter(kode -> !kode.equals(InnvilgelsesResultat.DELVIS_INNVILGET))
        .toList();

    @GetMapping("/folketrygden")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    public ResponseEntity<Map<String, Object>> hentKoderTilFolketrygden() {
        Map<String, Object> kodeverdier = new HashMap<>();
        kodeverdier.put(Vilkaar.class.getSimpleName(), tilKodeDto(Vilkaar.values()));
        kodeverdier.put(InnvilgelsesResultat.class.getSimpleName(), tilKodeDto(gyldigeInnvilgelsesResultat));
        kodeverdier.put("begrunnelser", lagBegrunnelser());
        return ResponseEntity.ok(kodeverdier);
    }

    private Map<String, Collection<KodeDto>> lagBegrunnelser() {
        Map<String, Collection<KodeDto>> begrunnelser = new HashMap<>();
        begrunnelser.put(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.class.getSimpleName(), tilKodeDto(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values()));
        begrunnelser.put(Ftrl_2_8_forutgaaende_trygdetid_begrunnelser.class.getSimpleName(), tilKodeDto(Ftrl_2_8_forutgaaende_trygdetid_begrunnelser.values()));
        begrunnelser.put(Medfolgende_barn_begrunnelser_ftrl.class.getSimpleName(), tilKodeDto(Medfolgende_barn_begrunnelser_ftrl.values()));
        begrunnelser.put(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.class.getSimpleName(), tilKodeDto(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.values()));
        begrunnelser.put(Ftrl_2_7_begrunnelser.class.getSimpleName(), tilKodeDto(Ftrl_2_7_begrunnelser.values()));
        return begrunnelser;
    }

    private <T extends Kodeverk> Collection<KodeDto> tilKodeDto(Collection<T> kodeverk) {
        return tilKodeDto(kodeverk.toArray(new Kodeverk[0]));
    }

    private Collection<KodeDto> tilKodeDto(Kodeverk... kodeverk) {
        return Stream.of(kodeverk).map(k -> new KodeDto(k.getKode(), k.getBeskrivelse())).toList();
    }
}
