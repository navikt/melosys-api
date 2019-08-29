package no.nav.melosys.saksflyt.steg.sed.sak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSaksrelasjon extends AbstraktStegBehandler {

    private final EessiService eessiService;

    public OppdaterSaksrelasjon(EessiService eessiService) {
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPDATER_SAKSRELASJON;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
        eessiService.lagreSaksrelasjon(
            fagsak.getGsakSaksnummer(),
            melosysEessiMelding.getRinaSaksnummer(),
            melosysEessiMelding.getBucType()
        );

        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
    }
}
