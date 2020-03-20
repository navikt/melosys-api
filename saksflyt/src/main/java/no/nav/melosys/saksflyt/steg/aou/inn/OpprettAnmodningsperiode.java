package no.nav.melosys.saksflyt.steg.aou.inn;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOpprettAnmodningsperiode")
public class OpprettAnmodningsperiode extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettAnmodningsperiode.class);

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final OpprettSedDokumentFelles opprettSedDokumentFelles;
    private final BehandlingService behandlingService;

    @Autowired
    public OpprettAnmodningsperiode(AnmodningsperiodeService anmodningsperiodeService,
                                    OpprettSedDokumentFelles opprettSedDokumentFelles,
                                    BehandlingService behandlingService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.opprettSedDokumentFelles = opprettSedDokumentFelles;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Ingen behandling finnes for prosessinstans " + prosessinstans.getId());
        }

        SedDokument sedDokument = hentEllerOpprettSedDokument(prosessinstans);
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode(sedDokument);
        anmodningsperiodeService.lagreAnmodningsperioder(prosessinstans.getBehandling().getId(), Collections.singletonList(anmodningsperiode));
        log.info("Opprettet anmodningsperiode for prosessinstans {}", prosessinstans.getId());

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SAK_OG_BEHANDLING_OPPRETTET);
    }

    private SedDokument hentEllerOpprettSedDokument(Prosessinstans prosessinstans) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Optional<SaksopplysningDokument> sedDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.SEDOPPL);

        if (sedDokument.isPresent()) {
            return (SedDokument) sedDokument.get();
        }

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        return (SedDokument) opprettSedDokumentFelles.opprettSedSaksopplysning(melosysEessiMelding, behandling).getDokument();
    }

    private static Anmodningsperiode lagAnmodningsperiode(SedDokument sedDokument) {
        return new Anmodningsperiode(
            sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom(),
            sedDokument.getLovvalgslandKode(),
            sedDokument.getLovvalgBestemmelse(),
            null,
            sedDokument.getUnntakFraLovvalgslandKode(),
            sedDokument.getUnntakFraLovvalgBestemmelse(),
            Trygdedekninger.UTEN_DEKNING
        );
    }
}
