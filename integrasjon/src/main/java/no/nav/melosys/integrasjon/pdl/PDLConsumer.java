package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;

public interface PDLConsumer {
    Identliste hentIdenter(String ident);
}
