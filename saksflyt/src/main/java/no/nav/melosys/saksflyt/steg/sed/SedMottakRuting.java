package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.AutomatiskSedBehandlingInitialiserer;
import no.nav.melosys.service.eessi.ManuellSedBehandlingInitialiserer;
import no.nav.melosys.service.eessi.RutingResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Transisjoner:
 * SED_MOTTAK_RUTING → SED_MOTTAK_FERDIGSTILL_JOURNALPOST om sed'en ikke støtter auto. behandling, eller om er en svar-sed (eks a002)
 * eller
 * SED_MOTTAK_RUTING → SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH om sak ikke finnes
 * eller
 * SED_MOTTAK_RUTING -> SED_MOTTAK_OPPRETT_NY_BEHANDLING hvis vi mottar en oppdatert sed. (eks a009)
 * eller
 * SED_MOTTAK_RUTING -> SED_MOTTAK_OPPRETT_JFR_OPPG hvis ny sed på ny buc, som ikke støtter auto. behandling
 */
@Component
public class SedMottakRuting extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SedMottakRuting.class);

    private final Collection<AutomatiskSedBehandlingInitialiserer> automatiskSedBehandlingInitialiserere;
    private final ManuellSedBehandlingInitialiserer manuellSedBehandlingInitialiserer;
    private final EessiService eessiService;

    @Autowired
    public SedMottakRuting(Collection<AutomatiskSedBehandlingInitialiserer> automatiskSedBehandlingInitialiserere,
                           ManuellSedBehandlingInitialiserer manuellSedBehandlingInitialiserer,
                           @Qualifier("system") EessiService eessiService) {
        this.automatiskSedBehandlingInitialiserere = automatiskSedBehandlingInitialiserere;
        this.manuellSedBehandlingInitialiserer = manuellSedBehandlingInitialiserer;
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_RUTING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Optional<Long> gsakSaksnummer = eessiService.finnSakForRinasaksnummer(melosysEessiMelding.getRinaSaksnummer());
        gsakSaksnummer.ifPresent(g -> prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, g));

        AutomatiskSedBehandlingInitialiserer automatiskSedBehandlingInitialiserer = hentInitialisererForSed(melosysEessiMelding);

        if (automatiskSedBehandlingInitialiserer != null) {
            //SED støtter automatisk behandling
            rutSedTilAutomatiskBehandling(prosessinstans, automatiskSedBehandlingInitialiserer, melosysEessiMelding, gsakSaksnummer.orElse(null));
        } else {
            manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);
        }

        if (inngangsSteg() == prosessinstans.getSteg()) {
            throw new TekniskException("Neste steg ikke oppdatert!");
        }

        log.info("Neste steg for SED {} fra rinasak {}: {}", melosysEessiMelding.getSedType(),
            melosysEessiMelding.getRinaSaksnummer(), prosessinstans.getSteg());
    }

    /*
    Ruter SED til korrekt behandling basert på om kriterier om sed'en er knyttet til en sak,
        er en oppdatert sed og/eller om perioden er oppdatert
     */
    private void rutSedTilAutomatiskBehandling(Prosessinstans prosessinstans,
                                               AutomatiskSedBehandlingInitialiserer automatiskSedBehandlingInitialiserer,
                                               MelosysEessiMelding melosysEessiMelding,
                                               Long gsakSaksnummer) throws TekniskException, FunksjonellException {
        RutingResultat resultat = automatiskSedBehandlingInitialiserer
            .finnSakOgBestemRuting(prosessinstans, gsakSaksnummer);

        if (resultat == RutingResultat.INGEN_BEHANDLING) {
            validerBehandlingErSatt(prosessinstans);
            prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);

        } else if (resultat == RutingResultat.OPPDATER_BEHANDLING) {
            validerBehandlingErSatt(prosessinstans);
            prosessinstans.setType(automatiskSedBehandlingInitialiserer.hentAktuellProsessType());
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);

        } else if (resultat == RutingResultat.NY_BEHANDLING) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, automatiskSedBehandlingInitialiserer.hentBehandlingstema(melosysEessiMelding));
            prosessinstans.setType(automatiskSedBehandlingInitialiserer.hentAktuellProsessType());
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING);

        } else if (resultat == RutingResultat.NY_SAK) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, automatiskSedBehandlingInitialiserer.hentBehandlingstema(melosysEessiMelding));
            prosessinstans.setType(automatiskSedBehandlingInitialiserer.hentAktuellProsessType());
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH);

        } else {
            throw new TekniskException("Ukjent Initialiseringsresultat: " + resultat);
        }

        log.info("Rutingresultat for SED-type {} fra rinasak {}: {}",
            melosysEessiMelding.getSedType(), melosysEessiMelding.getRinaSaksnummer(), resultat
        );
    }

    private void validerBehandlingErSatt(Prosessinstans prosessinstans) throws TekniskException {
        if (prosessinstans.getBehandling() == null) {
            throw new TekniskException("Prosessinstansen må ha en fagsak knyttet til seg for å kunne journalføre SED!");
        }
    }

    private AutomatiskSedBehandlingInitialiserer hentInitialisererForSed(MelosysEessiMelding melosysEessiMelding) {
        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());
        return automatiskSedBehandlingInitialiserere.stream()
            .filter(initialiserer -> initialiserer.gjelderSedType(sedType)).findFirst()
            .orElse(null);
    }
}
