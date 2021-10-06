package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Set;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;

public interface InngangsvilkaarConsumer {
    InngangsvilkarResponse vurderInngangsvilkår(Set<Land> brukersStatsborgerskap, Set<String> søknadsland, boolean erUkjenteEllerAlleEosLand, ErPeriode søknadsperiode);
}
