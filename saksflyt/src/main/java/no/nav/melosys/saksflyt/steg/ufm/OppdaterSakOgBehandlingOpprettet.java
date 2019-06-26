package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSakOgBehandlingOpprettet extends SakOgBehandlingStegBehander {

    protected OppdaterSakOgBehandlingOpprettet(SakOgBehandlingFasade sakOgBehandlingFasade) {
        super(sakOgBehandlingFasade);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        if (StringUtils.isEmpty(aktørId)) {
            throw new TekniskException("Aktørid finnes ikke for behandling " + behandlingId);
        }

        sakOgBehandlingOpprettet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_FERDIGSTILL_JOURNALPOST);
    }
}
