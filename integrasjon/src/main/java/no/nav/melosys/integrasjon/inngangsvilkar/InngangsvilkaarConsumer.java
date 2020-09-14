package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Collection;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;

public interface InngangsvilkaarConsumer {
    InngangsvilkarResponse vurderInngangsvilkår(Land brukersStatsborgerskap, Collection<String> søknadsland, ErPeriode søknadsperiode);
}
