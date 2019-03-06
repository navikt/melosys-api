package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;

public interface DokSysFasade {

    byte[] produserDokumentutkast(DokumentbestillingMetadata metadata, Object brevdata) throws IntegrasjonException;

    DokumentbestillingResponse produserIkkeredigerbartDokument(DokumentbestillingMetadata metadata, Object brevdata) throws FunksjonellException, TekniskException;
}
