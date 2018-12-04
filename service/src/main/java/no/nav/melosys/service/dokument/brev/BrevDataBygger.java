package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;

public interface BrevDataBygger {

    BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException;
}
