package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Set;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
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
    private final Unleash unleash;

    public InngangsvilkaarConsumerImpl(RestTemplate inngangsVilkaarRestTemplate,
                                       Unleash unleash) {
        this.restTemplate = inngangsVilkaarRestTemplate;
        this.unleash = unleash;
    }

    public InngangsvilkarResponse vurderInngangsvilkår(Set<Land> brukersStatsborgerskap, Set<String> søknadsland, ErPeriode søknadsperiode) {
        if (unleash.isEnabled("melosys.inngang.flere-statsborgerskap")) {
            var request = new VurderInngangsvilkaarRequest(brukersStatsborgerskap.stream().map(Land::getKode).collect(Collectors.toSet()),
                søknadsland, søknadsperiode);
            return restTemplate.postForObject("/inngangsvilkaar", new HttpEntity<>(request, getDefaultHeaders()), InngangsvilkarResponse.class);
        }
        var request = new VurderInngangsvilkaarEttStatsborgerskapRequest(brukersStatsborgerskap.stream().map(Land::getKode).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Statsborgerskap er påkrevd for å vurdere inngangsvilkår")),
            søknadsland, søknadsperiode);
        return restTemplate.postForObject("/inngangsvilkaar", new HttpEntity<>(request, getDefaultHeaders()), InngangsvilkarResponse.class);
    }
}
