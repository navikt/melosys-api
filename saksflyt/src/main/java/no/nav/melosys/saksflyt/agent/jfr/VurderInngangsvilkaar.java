package no.nav.melosys.saksflyt.agent.jfr;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.service.RegelmodulService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kaller regelmodulen for å vurdere inngangsvilkår. Setter type på fagsak basert på resultatet.
 *
 * Transisjoner:
 * JFR_VURDER_INNGANGSVILKÅR → HENT_ARBF_OPPL (eller til FEILET_MASKINELT hvis feil)
 */
@Component
public class VurderInngangsvilkaar extends AbstraktStegBehandler {
    
    private static final Logger log = LoggerFactory.getLogger(VurderInngangsvilkaar.class);

    private final RegelmodulService regelmodulService;
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public VurderInngangsvilkaar(RegelmodulService regelmodulService, FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.regelmodulService = regelmodulService;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        log.debug("InngangsvilkaarAgent initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utførSteg(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av {}", prosessinstans.getId());
        Behandling behandling = behandlingRepository.findOne(prosessinstans.getBehandling().getId());
        
        // Hent statsborgerskap fra saksopplysningene...
        // TODO (MELOSYS-1255): Statsborgerskap skal ikke hentes fra PersonopplysningsDokument, siden dette ikke nødvendigvis er riktig for søknadstidspunktet.
        Land statsborgerskap = null;
        for (Saksopplysning kandidat : behandling.getSaksopplysninger()) {
            if (kandidat.getDokument() instanceof PersonDokument) {
                statsborgerskap = ((PersonDokument) kandidat.getDokument()).statsborgerskap;
                break; // Forutsetter at vi har kun 1 av disse
            }
        }
        if (statsborgerskap == null) {
            log.error("Kunne ikke hente brukers statsborgerskap fra saksopplysningene.");
            // FIXME: MELOSYS-1316
            return;
        }

        // Kjør inngangsvilkår...
        log.debug("Kaller regelmodul for prosessinstans {}...", prosessinstans.getId());
        List<String> land = prosessinstans.getData(ProsessDataKey.LAND, List.class);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        if (periode == null || periode.getFom() == null) {
            log.error("Søknadsperioden er ikke oppgitt eller mangler fom");
            // FIXME: MELOSYS-1316
            return;
        }
        VurderInngangsvilkaarReply res = regelmodulService.vurderInngangsvilkår(statsborgerskap, land, periode);
        
        // Legg på evt. feil og varsler...
        boolean detErMeldtFeil = false;
        for (Feilmelding melding : res.feilmeldinger) {
            if (melding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL) {
                detErMeldtFeil = true;
            }
            log.info("Kall til regelmodul for prosessinstans {} returnerte {}", prosessinstans.getId(), melding.toString());
            prosessinstans.leggTilHendelse(inngangsSteg(), melding.kategori.name(), melding.melding);
        }
        
        // Håndter ev. feil...
        if (detErMeldtFeil) {
            log.info("Avbryter behandling av {} pga. feil", prosessinstans.getId());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            // FIXME: MELOSYS-1316
            return;
        }
        
        // Sett sakstype...
        Fagsak fagsak = behandling.getFagsak();
        FagsakType nyFagsakType = res.kvalifisererForEf883_2004 ? FagsakType.EU_EØS : FagsakType.FOLKETRYGD; // Fikses når inngangsvilkårsvurdering også kvalifiserer for avtaler.
        if (fagsak.getType() != null && fagsak.getType() != nyFagsakType) {
            log.error("Avbryter behandling av {}: Forsøk på å endre fagsakType fra {} til {}", prosessinstans.getId(), fagsak.getType(), nyFagsakType);
            // FIXME: MELOSYS-1316
            return;
        }
        log.info("Setter type på fagsak {} til {}", fagsak.getSaksnummer(), nyFagsakType); 
        fagsak.setType(nyFagsakType);
        fagsakRepository.save(fagsak);
        
        prosessinstans.setSteg(ProsessSteg.HENT_ARBF_OPPL);
        log.debug("Ferdig med behandling av {}", prosessinstans.getId());
    }
    
}
