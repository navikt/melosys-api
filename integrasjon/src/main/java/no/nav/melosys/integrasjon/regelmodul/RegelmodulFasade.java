package no.nav.melosys.integrasjon.regelmodul;

import java.util.List;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;

public interface RegelmodulFasade {
    VurderInngangsvilkaarReply vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, ErPeriode søknadsperiode) throws TekniskException;
}
