package no.nav.melosys.integrasjon.tps;

import java.util.Optional;

import no.nav.melosys.domain.Bruker;

public interface TpsFasade {

    Optional<Long> hentAktørIdForIdent(String fnr);

    Bruker hentKjerneinformasjon(Bruker bruker);
}
