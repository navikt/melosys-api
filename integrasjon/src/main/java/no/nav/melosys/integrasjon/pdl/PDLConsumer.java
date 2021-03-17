package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;

public interface PDLConsumer {
    Identliste hentIdenter(String ident) throws IkkeFunnetException, IntegrasjonException;
}
