package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sob.SobService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSakOgBehandlingOpprettet extends AbstraktStegBehandler {

    private final SobService sobService;

    @Autowired
    public OppdaterSakOgBehandlingOpprettet(SobService sobService) {
        this.sobService = sobService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        if (StringUtils.isEmpty(aktørId)) {
            throw new TekniskException("Aktørid finnes ikke for behandling " + behandlingId);
        }

        if (!Boolean.TRUE.equals(prosessinstans.getData(ProsessDataKey.ER_OPPDATERT_SED, Boolean.class))) {
            sobService.sakOgBehandlingOpprettet(saksnummer, behandlingId, aktørId);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE);
    }
}
