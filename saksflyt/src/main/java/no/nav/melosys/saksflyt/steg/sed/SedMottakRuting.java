package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter;
import no.nav.melosys.service.eessi.ruting.SedRuter;
import no.nav.melosys.service.eessi.ruting.SedRuterForSedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SedMottakRuting implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SedMottakRuting.class);

    private final Collection<SedRuterForSedType> ruterForSedTyper;
    private final DefaultSedRuter defaultSedRuter;
    private final EessiService eessiService;

    @Autowired
    public SedMottakRuting(Collection<SedRuterForSedType> ruterForSedTyper,
                           DefaultSedRuter defaultSedRuter,
                           @Qualifier("system") EessiService eessiService) {
        this.ruterForSedTyper = ruterForSedTyper;
        this.defaultSedRuter = defaultSedRuter;
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_RUTING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        log.info("Forsøker å rute sed {} i RINA-sak {}", eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());

        Long arkivsakID = eessiService.finnSakForRinasaksnummer(eessiMelding.getRinaSaksnummer()).orElse(null);
        SedType sedType = SedType.valueOf(eessiMelding.getSedType());
        hentSedRuterForSedType(sedType).rutSedTilBehandling(prosessinstans, arkivsakID);
    }

    private SedRuter hentSedRuterForSedType(SedType sedType) {
        return ruterForSedTyper.stream()
            .filter(sedRuter -> sedRuter.gjelderSedType(sedType))
            .findFirst()
            .map(SedRuter.class::cast)
            .orElse(defaultSedRuter);
    }
}
