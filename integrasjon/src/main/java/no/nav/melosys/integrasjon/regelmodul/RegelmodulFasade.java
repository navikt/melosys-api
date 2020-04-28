package no.nav.melosys.integrasjon.regelmodul;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;

public interface RegelmodulFasade {

    FastsettLovvalgReply fastsettLovvalg(Set<Saksopplysning> saksopplysninger);

    // FIXME: Gjør om til TekniskException
    VurderInngangsvilkaarReply vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, Periode søknadsperiode) throws RuntimeException;
}
