package no.nav.melosys.service.brev.bestilling;

import java.util.List;

import no.nav.melosys.domain.brev.NorskMyndighet;
import org.springframework.stereotype.Component;

@Component
public class HentTilgjengeligeNorskeMyndigheterService {
    public List<NorskMyndighet> hentTilgjengeligeNorskeMyndigheter() {
        return List.of(NorskMyndighet.SKATTEETATEN, NorskMyndighet.SKATTEINNKREVER_UTLAND, NorskMyndighet.HELFO);
    }
}
