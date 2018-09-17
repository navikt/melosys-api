package no.nav.melosys.tjenester.gui;

import no.nav.abac.xacml.NavAttributter;
import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.response.Advice;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;


@Service
public class Pep {

    private static final Logger abacLog = LoggerFactory.getLogger(Pep.class);

    public final static String READ = "read";
    public final static String IkkeTilgang = "Brukeren har ikke tilgang";

    @Autowired
    private AbacService abacService;

    @Autowired
    private AbacContext abacContext;

    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = Pep.READ))
    public void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException {
        abacContext.getRequest().resource(NavAttributter.RESOURCE_FELLES_PERSON_FNR, fnr);

        XacmlResponse accessResponse = abacService.evaluate(abacContext.getRequest());
        logResponse(fnr, accessResponse);

        if (accessResponse.getDecision() != Decision.PERMIT) {
            //throw new SikkerhetsbegrensningException(IkkeTilgang);
        }
    }

    public void logResponse(String fnr, XacmlResponse response) {
        String userId = SubjectHandler.getInstance().getUserID();
        String advices = getAdvicesAsString(response.getAdvices());
        abacLog.info(String.format("Ident %s spurte om ressurs %s med pdp-svar %s %s", userId, fnr, response.getDecision(), advices));
    }

    public String getAdvicesAsString(List<Advice> advices) {
        String advicesAsText = "";
        if (!advices.isEmpty()) {
            advicesAsText += " - Advices: ";
            advicesAsText += advices.stream()
                                .map(a -> a.toString())
                                .collect( Collectors.joining(", ") );
        }
        return advicesAsText;
    }
}