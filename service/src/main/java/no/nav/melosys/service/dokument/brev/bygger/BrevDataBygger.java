package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;

public interface BrevDataBygger {

    BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException;
}
