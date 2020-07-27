package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarSendSed")
public class SendSed extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendSed.class);

    private final EessiService eessiService;

    @Autowired
    public SendSed(@Qualifier("system") EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        long behandlingId = prosessinstans.getBehandling().getId();
        eessiService.sendAnmodningUnntakSvar(behandlingId);

        log.info("Svar på anmodning om unntak sendt for behandling {}", behandlingId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
