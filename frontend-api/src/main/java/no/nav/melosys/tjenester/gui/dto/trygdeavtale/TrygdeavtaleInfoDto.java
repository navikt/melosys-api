package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland;

public record TrygdeavtaleInfoDto(
    String aktoerId,
    String behandlingstema,
    String behandlingstype,
    boolean redigerbart,
    LocalDate periodeFom,
    LocalDate periodeTom,
    @JsonInclude(JsonInclude.Include.NON_NULL) Trygdeavtale_myndighetsland soeknadsland,
    List<VirksomhetDto> virksomheter, List<FamilieDto> barn,
    FamilieDto ektefelleSamboer,
    String innledningFritekst,
    String begrunnelseFritekst,
    String nyVurderingBakgrunn) {

    public TrygdeavtaleInfoDto(String aktoerId, String behandlingstema, String behandlingstype, boolean redigerbart, Periode periode, Trygdeavtale_myndighetsland soeknadsland, Map<String, String> virksomheter, List<MedfolgendeFamilie> familie, String innledingFritekst, String begrunnelseFritekst, String nyVurderingBakgrunn) {
        this(
            aktoerId,
            behandlingstema,
            behandlingstype,
            redigerbart,
            periode.getFom(),
            periode.getTom(),
            soeknadsland,
            mapVirksomheter(virksomheter),
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erBarn),
            filtrerOgMapFamilie(familie, MedfolgendeFamilie::erEktefelleSamboer)
                .stream().findFirst().orElse(null),
            innledingFritekst,
            begrunnelseFritekst,
            nyVurderingBakgrunn
        );
    }

    public static List<VirksomhetDto> mapVirksomheter(Map<String, String> virksomheter) {
        return virksomheter.entrySet().stream()
            .map(virksomhet -> new VirksomhetDto(virksomhet.getKey(), virksomhet.getValue()))
            .toList();
    }

    public static List<FamilieDto> filtrerOgMapFamilie(List<MedfolgendeFamilie> familie, Predicate<MedfolgendeFamilie> filterfunksjon) {
        return familie.stream()
            .filter(filterfunksjon)
            .map(familiemedlem -> new FamilieDto(familiemedlem.getUuid(), familiemedlem.getFnr(), familiemedlem.getNavn()))
            .toList();
    }
}
