package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.List;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InngangsvilkaarConsumerImpl implements InngangsvilkaarConsumer {

    private final RestTemplate restTemplate;

    public InngangsvilkaarConsumerImpl(RestTemplate inngangsVilkaarRestTemplate) {
        this.restTemplate = inngangsVilkaarRestTemplate;
    }

    public InngangsvilkarResponse vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, ErPeriode søknadsperiode) {
        var request = new VurderInngangsvilkaarRequest(brukersStatsborgerskap.getKode(), søknadsland, søknadsperiode);
        return restTemplate.postForObject("/inngangsvilkaar", request, InngangsvilkarResponse.class);
    }
}
