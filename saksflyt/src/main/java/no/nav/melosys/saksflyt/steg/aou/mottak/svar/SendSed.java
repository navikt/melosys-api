package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import java.util.Collection;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakSvarSendSed")
public class SendSed extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendSed.class);

    private final EessiService eessiService;
    private final AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    public SendSed(EessiService eessiService, AnmodningsperiodeService anmodningsperiodeService) {
        this.eessiService = eessiService;
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        long behandlingId = prosessinstans.getBehandling().getId();
        Collection<AnmodningsperiodeSvar> anmodningsperiodeSvar = anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(behandlingId);

        if (anmodningsperiodeSvar.size() != 1) {
            throw new FunksjonellException("Forventet én anmodningsperiode på behandling" + behandlingId + ", fant " + anmodningsperiodeSvar.size());
        }

        try {
            AnmodningsperiodeSvar svar = anmodningsperiodeSvar.iterator().next();
            eessiService.sendAnmodningUnntakSvar(svar, behandlingId);
        } catch (MelosysException e) {
            throw new TekniskException("Kunne ikke sende svar for anmodning om unntak", e);
        }

        log.info("Svar på anmodning om unntak sendt for behandling {}", behandlingId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL);
    }
}
