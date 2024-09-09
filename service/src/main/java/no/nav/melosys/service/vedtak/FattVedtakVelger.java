package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.springframework.stereotype.Component;

@Component
public class FattVedtakVelger {
    private final EosVedtakService eosVedtakService;
    private final FtrlVedtakService ftrlVedtakService;
    private final TrygdeavtaleVedtakService trygdeavtaleVedtakService;
    private final ÅrsavregningVedtakService årsavregningVedtakService;

    public FattVedtakVelger(EosVedtakService eosVedtakService,
                            FtrlVedtakService ftrlVedtakService,
                            TrygdeavtaleVedtakService trygdeavtaleVedtakService,
                            ÅrsavregningVedtakService årsavregningVedtakService) {
        this.eosVedtakService = eosVedtakService;
        this.ftrlVedtakService = ftrlVedtakService;
        this.trygdeavtaleVedtakService = trygdeavtaleVedtakService;
        this.årsavregningVedtakService = årsavregningVedtakService;
    }

    public FattVedtakInterface getFattVedtakService(Behandling behandling) {
        if (behandling.getType() == Behandlingstyper.ÅRSAVREGNING) {
            return årsavregningVedtakService;
        }

        return switch (behandling.getFagsak().getType()) {
            case EU_EOS -> eosVedtakService;
            case FTRL -> ftrlVedtakService;
            case TRYGDEAVTALE -> trygdeavtaleVedtakService;
        };
    }
}
