package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HentEessiMelding extends AbstraktStegBehandler {

    private final EessiService eessiService;
    private final TpsService tpsService;

    public HentEessiMelding(@Qualifier("system") EessiService eessiService, TpsService tpsService) {
        this.eessiService = eessiService;
        this.tpsService = tpsService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        String brukerID = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        String aktørID = tpsService.hentAktørIdForIdent(brukerID);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        if (prosessinstans.getType() == ProsessType.SED_GENERELL_SAK) {
            //SED'er journalført manuelt - oppretter ny generell sak
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH);
        } else {
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_RUTING);
        }
    }
}
