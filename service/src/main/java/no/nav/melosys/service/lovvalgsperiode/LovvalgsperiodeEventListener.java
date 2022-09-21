package no.nav.melosys.service.lovvalgsperiode;

import no.nav.melosys.domain.LovvalgsperiodeLagreEvent;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LovvalgsperiodeEventListener {

    private final LovvalgsperiodeService lovvalgsperiodeService;

    public LovvalgsperiodeEventListener(LovvalgsperiodeService lovvalgsperiodeService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @EventListener
    public void lagreLovvalgsperiode(LovvalgsperiodeLagreEvent lovvalgsperiodeLagreEvent) {
        ThreadLocalAccessInfo.executeProcess("lagreLovvalgsperiode", () ->
            lovvalgsperiodeService.lagreLovvalgsperioder(
                lovvalgsperiodeLagreEvent.getBehandlingId(),
                lovvalgsperiodeLagreEvent.getLovvalgsperioder())
        );
    }
}
