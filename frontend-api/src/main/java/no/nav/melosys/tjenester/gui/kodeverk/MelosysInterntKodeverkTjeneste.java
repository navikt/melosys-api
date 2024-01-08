package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.*;
import java.util.stream.Stream;

import io.getunleash.Unleash;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_forutgaaende_trygdetid_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;

@Protected
@RestController
@RequestMapping("/kodeverk/melosys-internt")
@Api(tags = {"kodeverk/melosys-internt"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MelosysInterntKodeverkTjeneste {

    private static final List<String> definedOrderTrygdedekning = List.of(
        FTRL_2_9_FØRSTE_LEDD_A_HELSE.getKode(),
        FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER.getKode(),
        FTRL_2_9_FØRSTE_LEDD_B_PENSJON.getKode(),
        FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON.getKode(),
        FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER.getKode());

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final BehandlingService behandlingService;
    private final Unleash unleash;

    public MelosysInterntKodeverkTjeneste(MedlemskapsperiodeService medlemskapsperiodeService, BehandlingService behandlingService, Unleash unleash) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.behandlingService = behandlingService;
        this.unleash = unleash;
    }

    @GetMapping("/folketrygden/{behandlingID}")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    public ResponseEntity<Map<String, Object>> hentKoderTilFolketrygden(@PathVariable("behandlingID") Long behandlingID) {
        Map<String, Object> kodeverdier = new HashMap<>();
        kodeverdier.put(Trygdedekninger.class.getSimpleName(), tilKodeDto(
            medlemskapsperiodeService.hentGyldigeTrygdedekninger(),
            Comparator.comparingInt(c -> definedOrderTrygdedekning.indexOf(c.getKode()))));
        kodeverdier.put(Vilkaar.class.getSimpleName(), tilKodeDto(Vilkaar.values()));
        kodeverdier.put(InnvilgelsesResultat.class.getSimpleName(), tilKodeDto(filtrerteInnvilgelsesResultat(behandlingID)));
        kodeverdier.put("begrunnelser", lagBegrunnelser());
        return ResponseEntity.ok(kodeverdier);
    }

    private List<InnvilgelsesResultat> filtrerteInnvilgelsesResultat(Long behandlingID) {
        return Arrays.stream(InnvilgelsesResultat.values())
            .filter(kode -> !kode.equals(InnvilgelsesResultat.DELVIS_INNVILGET))
            .filter(kode -> !kode.equals(InnvilgelsesResultat.OPPHØRT) ||
                (unleash.isEnabled(ToggleName.SAKSBEHANDLING_MANGLENDE_INNBETALING) && behandlingService.hentBehandling(behandlingID).erManglendeInnbetalingTrygdeavgift()))
            .toList();
    }

    private Map<String, Collection<KodeDto>> lagBegrunnelser() {
        Map<String, Collection<KodeDto>> begrunnelser = new HashMap<>();
        begrunnelser.put(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.class.getSimpleName(), tilKodeDto(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values()));
        begrunnelser.put(Ftrl_2_8_forutgaaende_trygdetid_begrunnelser.class.getSimpleName(), tilKodeDto(Ftrl_2_8_forutgaaende_trygdetid_begrunnelser.values()));
        begrunnelser.put(Medfolgende_barn_begrunnelser_ftrl.class.getSimpleName(), tilKodeDto(Medfolgende_barn_begrunnelser_ftrl.values()));
        begrunnelser.put(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.class.getSimpleName(), tilKodeDto(Medfolgende_ektefelle_samboer_begrunnelser_ftrl.values()));
        return begrunnelser;
    }

    private <T extends Kodeverk> List<KodeDto> tilKodeDto(Collection<T> kodeverk, Comparator<KodeDto> comparator) {
        return tilKodeDto(kodeverk).stream().sorted(comparator).toList();
    }

    private <T extends Kodeverk> Collection<KodeDto> tilKodeDto(Collection<T> kodeverk) {
        return tilKodeDto(kodeverk.toArray(new Kodeverk[0]));
    }

    private Collection<KodeDto> tilKodeDto(Kodeverk... kodeverk) {
        return Stream.of(kodeverk).map(k -> new KodeDto(k.getKode(), k.getBeskrivelse())).toList();
    }
}
