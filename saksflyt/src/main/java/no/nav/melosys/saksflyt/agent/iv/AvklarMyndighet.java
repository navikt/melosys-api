package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.LandvelgerService;
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
 *  IV_AVKLAR_MYNDIGHET -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvklarMyndighet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(no.nav.melosys.saksflyt.agent.iv.AvklarMyndighet.class);

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final FagsakService fagsakService;

    private final LandvelgerService landvelgerService;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public AvklarMyndighet(BehandlingRepository behandlingRepository,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           FagsakService fagsakService,
                           LandvelgerService landvelgerService,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.fagsakService = fagsakService;
        this.landvelgerService = landvelgerService;
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

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findWithSaksbehandlingById(behandlingID)
            .orElseThrow(() -> new TekniskException("Behandlingsresultat " + behandlingID + " finnes ikke."));

        if (SendBrevValidator.innvilgelsesbrevSkalSendes(behandlingsresultat.getType(), behandlingsresultat.getLovvalgsperioder().iterator().next())) {
            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            String saksnummer = fagsak.getSaksnummer();
            Aktoer myndighetPart = fagsak.hentAktørMedRolleType(Aktoersroller.MYNDIGHET);
            if (myndighetPart == null) {
                Landkoder landkode = landvelgerService.hentTrygdemyndighetsland(behandling);
                UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
                String institusjonsID = landkode.getKode() + ":" + myndighet.institusjonskode;
                fagsakService.leggTilAktør(saksnummer, Aktoersroller.MYNDIGHET, institusjonsID);
                log.info("Avklart landkode {} og myndighet {} for sak {}.", landkode, myndighet.institusjonskode, saksnummer);
            } else {
                log.debug("Sak {} har allerede en myndighet med kode {}", saksnummer, myndighetPart.getInstitusjonId());
            }
        }

        prosessinstans.setSteg(IV_OPPDATER_MEDL);
    }
}