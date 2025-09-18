package no.nav.melosys.sikkerhet.abac;

import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.stereotype.Service;

import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;

@Service
public class PepImpl implements Pep {
    private static final Logger abacLog = LoggerFactory.getLogger(PepImpl.class);

    public static final String READ = "read";
    private static final String IKKE_TILGANG = "ABAC: Brukeren har ikke tilgang til ressurs";

    private AbacService abacService;
    private AbacContext abacContext;

    public PepImpl(AbacService abacService, AbacContext abacContext) {
        this.abacService = abacService;
        this.abacContext = abacContext;
    }

    @Override
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    public void sjekkTilgangTilFnr(String fnr) {
        abacContext.getRequest().resource(NavAttributter.RESOURCE_FELLES_PERSON_FNR, fnr);
        evaluer(abacContext.getRequest(), fnr);
    }

    @Override
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    public void sjekkTilgangTilAktoerId(String aktoerId) {
        abacContext.getRequest().resource(NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId);
        evaluer(abacContext.getRequest(), aktoerId);
    }

    private void evaluer(XacmlRequest request, String id) {
        XacmlResponse accessResponse = abacService.evaluate(request);

        if (accessResponse.getDecision() != Decision.PERMIT) {
            if (abacLog.isWarnEnabled()) {
                abacLog.warn(createLogString(id, accessResponse));
            }
            throw new SikkerhetsbegrensningException(IKKE_TILGANG);
        }

        if (abacLog.isInfoEnabled()) {
            abacLog.info(createLogString(id, accessResponse));
        }
    }

    private String createLogString(String fnr, XacmlResponse response) {
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
                .collect(Collectors.joining(", "));
        }
        return advicesAsText;
    }
}
