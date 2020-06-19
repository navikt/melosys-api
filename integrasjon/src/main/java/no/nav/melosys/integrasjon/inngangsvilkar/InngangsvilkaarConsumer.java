package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.List;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;

public interface InngangsvilkaarConsumer {
    public InngangsvilkarResponse vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, ErPeriode søknadsperiode);
}
