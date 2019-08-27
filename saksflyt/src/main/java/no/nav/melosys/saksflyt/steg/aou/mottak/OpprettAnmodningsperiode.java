package no.nav.melosys.saksflyt.steg.aou.mottak;

import java.util.Collections;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakOpprettAnmodningsperiode")
public class OpprettAnmodningsperiode extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettAnmodningsperiode.class);

    private final AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    public OpprettAnmodningsperiode(AnmodningsperiodeService anmodningsperiodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();
        if (behandling == null) {
            throw new FunksjonellException("Ingen behandling finnes for prosessinstans " + prosessinstans.getId());
        }

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode(melosysEessiMelding);
        anmodningsperiodeService.lagreAnmodningsperioder(behandling.getId(), Collections.singletonList(anmodningsperiode));
        log.info("Opprettet anmodningsperiode for prosessinstans {}", prosessinstans.getId());

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL);
    }

    private static Anmodningsperiode lagAnmodningsperiode(MelosysEessiMelding melosysEessiMelding) {
        AnmodningUnntak anmodningUnntak = melosysEessiMelding.getAnmodningUnntak();

        return new Anmodningsperiode(
            melosysEessiMelding.getPeriode().getFom(),
            melosysEessiMelding.getPeriode().getTom(),
            Landkoder.valueOf(melosysEessiMelding.getLovvalgsland()),
            LovvalgTilBestemmelseDtoMapper.mapBestemmelseVerdiTilMelosysLovvalgBestemmelse(melosysEessiMelding.getArtikkel()),
            null,
            Landkoder.valueOf(anmodningUnntak.getUnntakFraLovvalgsland()),
            LovvalgTilBestemmelseDtoMapper.mapBestemmelseVerdiTilMelosysLovvalgBestemmelse(anmodningUnntak.getUnntakFraLovvalgsbestemmelse()),
            Trygdedekninger.UTEN_DEKNING
        );
    }
}
