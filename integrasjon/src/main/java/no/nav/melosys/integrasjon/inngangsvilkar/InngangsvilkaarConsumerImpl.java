package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Set;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InngangsvilkaarConsumerImpl implements InngangsvilkaarConsumer, JsonRestIntegrasjon {

    private final RestTemplate restTemplate;

    public InngangsvilkaarConsumerImpl(RestTemplate inngangsVilkaarRestTemplate) {
        this.restTemplate = inngangsVilkaarRestTemplate;
    }

    public InngangsvilkarResponse vurderInngangsvilkår(Land brukersStatsborgerskap, Set<String> søknadsland, ErPeriode søknadsperiode) {
        var request = new VurderInngangsvilkaarRequest(brukersStatsborgerskap.getKode(), søknadsland, søknadsperiode);
        return restTemplate.postForObject("/inngangsvilkaar", new HttpEntity<>(request, getDefaultHeaders()), InngangsvilkarResponse.class);
    }
}
