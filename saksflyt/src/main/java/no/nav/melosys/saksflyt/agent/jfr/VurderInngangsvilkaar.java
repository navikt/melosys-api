package no.nav.melosys.saksflyt.agent.jfr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.RegelmodulService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.feil.Feilkategori.*;

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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void utførSteg(Prosessinstans prosessinstans) {
        log.debug("Starter behandling av {}", prosessinstans.getId());
        Behandling behandling = behandlingRepository.findOne(prosessinstans.getBehandling().getId());

        try {
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
                log.error("Funksjonell feil for {}: Kunne ikke hente brukers statsborgerskap fra saksopplysningene.", prosessinstans.getId());
                håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Ingen informasjon om statsborgerskap", null);
                return;
            }

            // Kjør inngangsvilkår...
            List<String> oppholdsland = prosessinstans.getData(ProsessDataKey.LAND, List.class);
            // FIXME MELOSYS-1377 Regelmodulen jobber med ISO 3 landkoder (oppholdsland må konverteres)
            oppholdsland = tilIso3Landkoder(oppholdsland);
            Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
            if (periode == null || periode.getFom() == null) {
                log.error("Funksjonell feil for {}: Søknadsperioden er ikke oppgitt eller mangler fom.", prosessinstans.getId());
                håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Søknadsperioden er ikke oppgitt eller mangler fom.", null);
                return;
            }
            log.debug("Kaller regelmodul for prosessinstans {}...", prosessinstans.getId());
            VurderInngangsvilkaarReply res = regelmodulService.vurderInngangsvilkår(statsborgerskap, oppholdsland, periode);

            // Legg på evt. feil og varsler...
            boolean detErMeldtFeil = false;
            for (Feilmelding melding : res.feilmeldinger) {
                if (melding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL) {
                    detErMeldtFeil = true;
                }
                log.info("Kall til regelmodul for prosessinstans {} returnerte {}", prosessinstans.getId(), melding.toString());
                prosessinstans.leggTilHendelse(melding.kategori.name(), melding.melding);
            }

            // Håndter ev. feil...
            if (detErMeldtFeil) {
                log.info("Avbryter behandling av {} pga. feil", prosessinstans.getId());
                håndterUnntak(TEKNISK_FEIL, prosessinstans, "Uventet feil fra regelmodulen", null);
                return;
            }

            // Sett sakstype...
            Fagsak fagsak = behandling.getFagsak();
            FagsakType nyFagsakType = res.kvalifisererForEf883_2004 ? FagsakType.EU_EØS : FagsakType.FOLKETRYGD; // Fikses når inngangsvilkårsvurdering også kvalifiserer for avtaler.
            if (fagsak.getType() != null && fagsak.getType() != nyFagsakType) {
                log.error("Avbryter behandling av {}: Forsøk på å endre fagsakType fra {} til {}", prosessinstans.getId(), fagsak.getType(), nyFagsakType);
                håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Forsøk på å endre fagsakType fra " + fagsak.getType() + " til " + nyFagsakType, null);
                return;
            }
            log.info("Setter type på fagsak {} til {}", fagsak.getSaksnummer(), nyFagsakType); 
            fagsak.setType(nyFagsakType);
            fagsakRepository.save(fagsak);

            prosessinstans.setSteg(ProsessSteg.HENT_ARBF_OPPL);
        } catch (RuntimeException e) {
            håndterUnntak(UVENTET_EXCEPTION, prosessinstans, "Uventet RuntimeException", e);
        }

        log.debug("Ferdig med behandling av {}", prosessinstans.getId());
    }

    // FIXME MELOSYS-1377 Regelmodulen jobber med ISO 3 landkoder.
    private List<String> tilIso3Landkoder(List<String> oppholdsland) {
        List<String> landkoder = new ArrayList<>();
        oppholdsland.forEach(l -> landkoder.add(tilIso3(l)));
        return landkoder;
    }

    // Midlertidig fiks
    private String tilIso3(String l) {
        Landkoder iso2Kode = Landkoder.valueOf(l);

        switch (iso2Kode) {
            case BE: return Land.BELGIA;
            case NO: return Land.NORGE;
            case BG: return Land.BULGARIA;
            case CZ: return Land.TSJEKKIA;
            case DK: return Land.DANMARK;
            case EE: return Land.ESTLAND;
            case FI: return Land.FINLAND;
            case FR: return Land.FRANKRIKE;
            case GR: return Land.HELLAS;
            case IE: return Land.IRLAND;
            case IS: return Land.ISLAND;
            case IT: return Land.ITALIA;
            case HR: return Land.UNGARN;
            case CY: return Land.KYPROS;
            case LV: return Land.LATVIA;
            case LI: return Land.LIECHTENSTEIN;
            case LT: return Land.LITAUEN;
            case LU: return Land.LUXEMBOURG;
            case MT: return Land.MALTA;
            case NL: return Land.NEDERLAND;
            case PL: return Land.POLEN;
            case PT: return Land.PORTUGAL;
            case RO: return Land.ROMANIA;
            case SK: return Land.SLOVAKIA;
            case SI: return Land.SLOVENIA;
            case ES: return Land.SPANIA;
            case GB: return Land.STORBRITANNIA;
            case SE: return Land.SVERIGE;
            case DE: return Land.TYSKLAND;
            case HU: return Land.UNGARN;
            case AT: return Land.ØSTERRIKE;
            default: throw new IllegalArgumentException(iso2Kode.getKode());
        }
    }
}
