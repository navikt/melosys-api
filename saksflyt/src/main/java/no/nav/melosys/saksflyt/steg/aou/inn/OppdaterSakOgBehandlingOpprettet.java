package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOppdaterSakOgBehandlingOpprettet")
public class OppdaterSakOgBehandlingOpprettet implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterSakOgBehandlingOpprettet.class);

    private final SobService sobService;

    @Autowired
    public OppdaterSakOgBehandlingOpprettet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SAK_OG_BEHANDLING_OPPRETTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        long behandlingId = prosessinstans.getBehandling().getId();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        if (StringUtils.isEmpty(aktørId)) {
            throw new TekniskException("Aktørid finnes ikke for behandling " + behandlingId);
        }

        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        sobService.sakOgBehandlingOpprettet(saksnummer, behandlingId, aktørId);

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE);
    }
}
