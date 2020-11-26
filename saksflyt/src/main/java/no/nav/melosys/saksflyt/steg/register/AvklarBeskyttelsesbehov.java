package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvklarBeskyttelsesbehov implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvklarBeskyttelsesbehov.class);

    private final BehandlingService behandlingService;
    private final TpsFasade tpsFasade;

    @Autowired
    public AvklarBeskyttelsesbehov(BehandlingService behandlingService, TpsFasade tpsFasade) {
        this.behandlingService = behandlingService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AVKLAR_BESKYTTELSESBEHOV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        if (behandling.hentPersonDokument().diskresjonskode.erKode6()) {
            prosessinstans.setData(ProsessDataKey.HAR_SENSITIVE_OPPLYSNINGER, true);
        } else {
            List<String> fnrMedfølgendeBarn = behandling.getBehandlingsgrunnlag()
                .getBehandlingsgrunnlagdata().personOpplysninger.hentFnrMedfølgendeBarn();
            for (String fnr : fnrMedfølgendeBarn) {
                if (tpsFasade.harSensitiveOpplysninger(fnr)) {
                    prosessinstans.setData(ProsessDataKey.HAR_SENSITIVE_OPPLYSNINGER, true);
                    break;
                }
            }
        }
        log.info("Avklart beskyttelsesbehov for person i behandling {}", behandling.getId());
    }
}
