package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface DokSysFasade {

    byte[] produserDokumentutkast(DokumentbestillingMetadata request, Object brevdata) throws IntegrasjonException;

    DokumentbestillingResponse produserIkkeredigerbartDokument(DokumentbestillingMetadata request, Object brevdata) throws SikkerhetsbegrensningException, IntegrasjonException;
}
