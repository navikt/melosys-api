package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;

/**
 * Avklarer hvilken utenlandsk myndighet er part i saken.
 *
 * Transisjoner:
 * IV_AVKLAR_MYNDIGHET -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvklarMyndighet extends AbstraktStegBehandler {

        private static final Logger log = LoggerFactory.getLogger(no.nav.melosys.saksflyt.agent.iv.AvklarMyndighet.class);

        private final AvklarteFaktaRepository avklarteFaktaRepository;

        private final BehandlingsresultatRepository behandlingsresultatRepository;

        private final FagsakService fagsakService;

        private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

        @Autowired
        public AvklarMyndighet(AvklarteFaktaRepository avklarteFaktaRepository,
                               BehandlingsresultatRepository behandlingsresultatRepository,
                               FagsakService fagsakService,
                               UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
            this.avklarteFaktaRepository = avklarteFaktaRepository;
            this.behandlingsresultatRepository = behandlingsresultatRepository;
            this.fagsakService = fagsakService;
            this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
            log.info("AvklarMyndighet initialisert");
        }

        @Override
        public ProsessSteg inngangsSteg() {
            return IV_AVKLAR_MYNDIGHET;
        }

        @Override
        protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
            return FeilStrategi.standardFeilHåndtering();
        }

        @Override
        public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
            log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

            Long behandlingsresultatID = prosessinstans.getBehandling().getId();
            Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingsresultatID)
                .orElseThrow(() -> new TekniskException("Behandlingsresultat " + behandlingsresultatID + " finnes ikke."));

            if (SendBrevValidator.innvilgelsesbrevSkalSendes(behandlingsresultat.getType(), behandlingsresultat.getLovvalgsperioder().iterator().next())) {
                Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
                String saksnummer = fagsak.getSaksnummer();
                Aktoer myndighetPart = fagsak.hentAktørMedRolleType(Aktoersroller.MYNDIGHET);
                if (myndighetPart == null) {
                    Landkoder landkode = avklarLand(behandlingsresultatID);
                    UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
                    fagsakService.leggTilAktør(saksnummer, Aktoersroller.MYNDIGHET, myndighet.institusjonskode);
                    log.info("Avklart landkode {} og myndighet {} for sak {}.", landkode, myndighet.institusjonskode, saksnummer);
                } else {
                    log.debug("Sak {} har allerede en myndighet med kode {}", saksnummer, myndighetPart.getUtenlandskId()); // FIXME institusjonsID
                }
            }

            prosessinstans.setSteg(IV_OPPDATER_MEDL);
        }

    /**
     * Brev til utenlandske trygdemyndigheter skal sendes til arbeidsland/oppholdsland.
     * •	Ved hjemmebase Norge så sender vi A1/SED til bostedslandet. Strengt tatt så skal vi også utstede A1 når personen bor i Norge, men vi sender ikke kopi til andre trygdemyndigheter.
     * •	Ved arbeid på skip flaggland Norge, sender vi A1/SED til bostedslandet.
     * •	Ved arbeid på skip med annet flagg, sender vi A1/SED til flagglandet dersom personen og arbeidsgiver er i Norge.
     */
    Landkoder avklarLand(Long behandlingsresultatID) throws FunksjonellException {
        //FIXME venter på noen avklaringer
        Avklartefakta arbeidsland = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsresultatID, Avklartefaktatype.ARBEIDSLAND)
            .orElseThrow(() -> new FunksjonellException("Avklartefakta ARBEIDSLAND mangler for behandlingsresultat " + behandlingsresultatID)) ;
        return Landkoder.valueOf(arbeidsland.getFakta());
    }
}
