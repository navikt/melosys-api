package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import org.w3c.dom.Element;

public interface DoksysFasade {

    byte[] produserDokumentutkast(DokumentbestillingMetadata metadata, Element brevdata) throws IntegrasjonException;

    DokumentbestillingResponse produserIkkeredigerbartDokument(DokumentbestillingMetadata metadata, Element brevdata) throws FunksjonellException, TekniskException;
}
