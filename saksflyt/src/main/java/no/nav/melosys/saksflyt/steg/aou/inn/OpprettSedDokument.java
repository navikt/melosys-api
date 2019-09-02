package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOpprettSedDokument")
public class OpprettSedDokument extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final OpprettSedDokumentFelles opprettSedDokumentFelles;

    @Autowired
    public OpprettSedDokument(OpprettSedDokumentFelles opprettSedDokumentFelles) {
        this.opprettSedDokumentFelles = opprettSedDokumentFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_SEDDOKUMENT;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        opprettSedDokumentFelles.opprettSedSaksopplysning(melosysEessiMelding, prosessinstans.getBehandling());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_HENT_PERSON);
    }
}
