package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;

public interface BrevDataBygger {

    BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException;
}
