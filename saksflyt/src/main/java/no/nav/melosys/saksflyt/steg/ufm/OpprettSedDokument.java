package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final OpprettSedDokumentFelles opprettSedDokumentFelles;

    @Autowired
    public OpprettSedDokument(OpprettSedDokumentFelles opprettSedDokumentFelles) {
        this.opprettSedDokumentFelles = opprettSedDokumentFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        opprettSedDokumentFelles.opprettSedSaksopplysning(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class), prosessinstans.getBehandling());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_REGISTEROPPLYSNINGER);
    }
}
