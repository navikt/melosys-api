package no.nav.melosys.service.brev.components;

import java.util.List;

import no.nav.melosys.domain.brev.Etat;
import org.springframework.stereotype.Component;

@Component
public class HentTilgjengeligeEtaterComponent {
    public List<Etat> hentTilgjengeligeEtater() {
        return List.of(Etat.SKATTEETATEN_ORGNR, Etat.SKATTINNKREVER_UTLAND_ORGNR, Etat.HELFO_ORGNR);
    }
}
