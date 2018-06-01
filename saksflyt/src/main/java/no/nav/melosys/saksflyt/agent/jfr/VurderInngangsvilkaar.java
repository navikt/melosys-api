package no.nav.melosys.saksflyt.agent.jfr;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.RegelmodulService;

/**
 * Kaller regelmodulen for å vurdere inngangsvilkår. Setter type på fagsak basert på resultatet.
 *
 * Transisjoner:
 * JFR_VURDER_INNGANGSVILKÅR → JFR_OPPRETT_OPPGAVE (eller til FEILET_MASKINELT hvis feil)
 */
public class VurderInngangsvilkaar extends StandardAbstraktAgent {
    
    private static final Logger log = LoggerFactory.getLogger(VurderInngangsvilkaar.class);

    private RegelmodulService regelmodulService;
    private FagsakService fagsakService;

    @Autowired
    public VurderInngangsvilkaar(Binge binge, ProsessinstansRepository prosessinstansRepo, RegelmodulService regelmodulService, FagsakService fagsakService) {
        super(binge, prosessinstansRepo);
        this.regelmodulService = regelmodulService;
        this.fagsakService = fagsakService;
        log.debug("InngangsvilkaarAgent initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utførSteg(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av {}", prosessinstans.getId());
        
        // Hent statsborgerskap fra saksopplysningene...
        // TODO (MELOSYS-1255): Statsborgerskap skal ikke hentes fra PersonopplysningsDokument, siden dette ikke nødvendigvis er riktig for søknadstidspunktet.
        Land statsborgerskap = null;
        for (Saksopplysning kandidat : prosessinstans.getBehandling().getSaksopplysninger()) {
            if (kandidat.getDokument() instanceof PersonDokument) {
                statsborgerskap = ((PersonDokument) kandidat.getDokument()).statsborgerskap;
                break; // Forutsetter at vi har kun 1 av disse
            }
        }
        if (statsborgerskap == null) {
            log.error("Kunne ikke hente brukers statsborgerskap fra saksopplysningene.");
            håndterFeil(prosessinstans, false);
            return;
        }

        // Kjør inngangsvilkår...
        log.debug("Kaller regelmodul for prosessinstans {}...", prosessinstans.getId());
        List<String> land = prosessinstans.getData(ProsessDataKey.LAND, List.class);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        if (periode == null || periode.getFom() == null) {
            log.error("Søknadsperioden er ikke oppgitt eller mangler fom");
            håndterFeil(prosessinstans, false);
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
            håndterFeil(prosessinstans, false);
            return;
        }
        
        // Sett sakstype...
        Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
        FagsakType nyFagsakType = res.kvalifisererForEf883_2004 ? FagsakType.EU_EØS : FagsakType.FOLKETRYGD; // Fikses når inngangsvilkårsvurdering også kvalifiserer for avtaler.
        if (fagsak.getType() != null && fagsak.getType() != nyFagsakType) {
            log.error("Avbryter behandling av {}: Forsøk på å endre fagsakType fra {} til {}", prosessinstans.getId(), fagsak.getType(), nyFagsakType);
            håndterFeil(prosessinstans, false);
            return;
        }
        log.info("Setter type på fagsak {} til {}", fagsak.getSaksnummer(), nyFagsakType); 
        fagsak.setType(nyFagsakType);
        fagsakService.lagre(fagsak);
        
        prosessinstans.setSteg(ProsessSteg.JFR_OPPRETT_OPPGAVE);
        log.debug("Ferdig med behandling av {}", prosessinstans.getId());
    }
    
}
