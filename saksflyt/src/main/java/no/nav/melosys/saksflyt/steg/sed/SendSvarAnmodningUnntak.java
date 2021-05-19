package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SendSvarAnmodningUnntak implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendSvarAnmodningUnntak.class);

    private final EessiService eessiService;

    @Autowired
    public SendSvarAnmodningUnntak(@Qualifier("system") EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_SVAR_ANMODNING_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        long behandlingId = prosessinstans.getBehandling().getId();
        eessiService.sendAnmodningUnntakSvar(behandlingId, prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
        log.info("Svar på anmodning om unntak sendt for behandling {}", behandlingId);
    }
}
