package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        opprettSedDokumentFelles.opprettSedSaksopplysning(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class), prosessinstans.getBehandling());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }
}
