package no.nav.melosys.integrasjon.gsak;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;

public interface GsakFasade {

    String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException;
}
