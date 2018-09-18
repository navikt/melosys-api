package no.nav.melosys.service;

import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;

public interface Pep {
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException;

    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    void sjekkTilgangTil(Aktoer bruker) throws SikkerhetsbegrensningException, IkkeFunnetException;
}
