package no.nav.melosys.service.lovvalgsperiode;

import no.nav.melosys.domain.LovvalgsperiodeLagreEvent;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LovvalgsperiodeEventListener {

    private static final Logger log = LoggerFactory.getLogger(LovvalgsperiodeEventListener.class);
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public LovvalgsperiodeEventListener(LovvalgsperiodeService lovvalgsperiodeService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @EventListener
    public void lagreLovvalgsperiode(LovvalgsperiodeLagreEvent lovvalgsperiodeLagreEvent) {
        log.debug("[Event] lagreLovvalgsperiode: {}", lovvalgsperiodeLagreEvent.toString());
        ThreadLocalAccessInfo.executeProcess("lagreLovvalgsperiode", () -> lovvalgsperiodeService.lagreLovvalgsperioder(lovvalgsperiodeLagreEvent.getBehandlingId(),
            lovvalgsperiodeLagreEvent.getLovvalgsperioder()));
    }
}
