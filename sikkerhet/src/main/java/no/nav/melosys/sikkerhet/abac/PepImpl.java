package no.nav.melosys.sikkerhet.abac;

import no.nav.abac.xacml.NavAttributter;
import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
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
public class PepImpl implements Pep {

    private static final Logger abacLog = LoggerFactory.getLogger(PepImpl.class);

    public final static String READ = "read";
    public final static String WRITE = "write";
    public final static String IkkeTilgang = "Brukeren har ikke tilgang";

    private AbacService abacService;
    private AbacContext abacContext;

    @Autowired
    public PepImpl(AbacService abacService, AbacContext abacContext) {
        this.abacService = abacService;
        this.abacContext = abacContext;
    }

    @Override
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    public void sjekkTilgangTilFnr(String fnr) throws SikkerhetsbegrensningException {
        abacContext.getRequest().resource(NavAttributter.RESOURCE_FELLES_PERSON_FNR, fnr);
        evaluer(abacContext.getRequest(), fnr);
    }

    @Override
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    public void sjekkTilgangTilAktoerId(String aktoerId) throws SikkerhetsbegrensningException {
        abacContext.getRequest().resource(NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId);
        evaluer(abacContext.getRequest(), aktoerId);
    }

    private void evaluer(XacmlRequest request, String id) throws SikkerhetsbegrensningException {
        XacmlResponse accessResponse = abacService.evaluate(request);

        if (accessResponse.getDecision() != Decision.PERMIT) {
            abacLog.warn(createLogString(id, accessResponse));
            throw new SikkerhetsbegrensningException(IkkeTilgang);
        }
        abacLog.info(createLogString(id, accessResponse));
    }

    private String createLogString(String fnr, XacmlResponse response){
        String userId = SubjectHandler.getInstance().getUserID();
        String advices = getAdvicesAsString(response.getAdvices());
        return String.format("Ident %s spurte om ressurs %s med pdp-svar %s %s", userId, fnr, response.getDecision(), advices);
    }

    private String getAdvicesAsString(List<Advice> advices) {
        String advicesAsText = "";
        if (!advices.isEmpty()) {
            advicesAsText += " - Advices: ";
            advicesAsText += advices.stream()
                                .map(Advice::toString)
                                .collect( Collectors.joining(", ") );
        }
        return advicesAsText;
    }
}