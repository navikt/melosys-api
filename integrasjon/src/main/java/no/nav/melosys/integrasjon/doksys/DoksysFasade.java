package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;

public interface DoksysFasade {

    byte[] produserDokumentutkast(Dokumentbestilling dokumentbestilling) throws IntegrasjonException;

    DokumentbestillingResponse produserIkkeredigerbartDokument(Dokumentbestilling dokumentbestilling) throws FunksjonellException, TekniskException;
}
