package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface DokSysFasade {

    byte[] produserDokumentutkast() throws IntegrasjonException;

    String produserIkkeredigerbartDokument() throws SikkerhetsbegrensningException, IntegrasjonException;
}
