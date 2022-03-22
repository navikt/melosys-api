package no.nav.melosys.saksflyt.steg.sed.mottak;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter;
import no.nav.melosys.service.eessi.ruting.SedRuter;
import no.nav.melosys.service.eessi.ruting.SedRuterForSedTyper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SedMottakRuting implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SedMottakRuting.class);

    private final Collection<SedRuterForSedTyper> sedRutereForSedTyper;
    private final DefaultSedRuter defaultSedRuter;
    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;

    public SedMottakRuting(Collection<SedRuterForSedTyper> ruterForSedTyper,
                           DefaultSedRuter defaultSedRuter,
                           @Qualifier("system") EessiService eessiService,
                           @Qualifier("system") JoarkFasade joarkFasade) {
        this.defaultSedRuter = defaultSedRuter;
        this.eessiService = eessiService;
        this.sedRutereForSedTyper = ruterForSedTyper;
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_RUTING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        final String journalpostID = eessiMelding.getJournalpostId();

        if (joarkFasade.hentJournalpost(journalpostID).isErFerdigstilt()) {
            log.warn("Journalpost {} for sed {} i RINA-sak {} er allerede ferdigstilt. Behandler ikke videre",
                journalpostID, eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());
        } else {
            log.info("Forsøker å rute sed {} i RINA-sak {}", eessiMelding.getSedId(), eessiMelding.getRinaSaksnummer());

            Long arkivsakID = eessiService.finnSakForRinasaksnummer(eessiMelding.getRinaSaksnummer()).orElse(null);
            var sedType = SedType.valueOf(eessiMelding.getSedType());
            hentSedRuterForSedType(sedType).rutSedTilBehandling(prosessinstans, arkivsakID);
        }
    }

    private SedRuter hentSedRuterForSedType(SedType sedType) {
        return Optional.ofNullable(hentSedRutereForSedTyper().get(sedType))
            .map(SedRuter.class::cast)
            .orElse(defaultSedRuter);
    }

    private Map<SedType, SedRuterForSedTyper> hentSedRutereForSedTyper() {
        return sedRutereForSedTyper.stream()
            .flatMap(ruter -> ruter.gjelderSedTyper().stream().map(sedType -> Map.entry(sedType, ruter)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
