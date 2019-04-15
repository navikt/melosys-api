package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
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

import static no.nav.melosys.feil.Feilkategori.FUNKSJONELL_FEIL;


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
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());

        // Hent statsborgerskap fra saksopplysningene...
        // Ved søknad tilbake i tid brukes historisk statsborgerskap
        boolean brukHistoriskStatsborgerskap = false;
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class); // Allerede validert

        if (periode.getFom().isBefore(LocalDate.now())) {
            brukHistoriskStatsborgerskap = true;
        }

        Land statsborgerskap = null;
        for (Saksopplysning kandidat : behandling.getSaksopplysninger()) {
            if (!brukHistoriskStatsborgerskap && kandidat.getDokument() instanceof PersonDokument) {
                statsborgerskap = ((PersonDokument) kandidat.getDokument()).statsborgerskap;
                break;
            }
            if (brukHistoriskStatsborgerskap && kandidat.getDokument() instanceof PersonhistorikkDokument) {
                PersonhistorikkDokument personhistorikk = (PersonhistorikkDokument) kandidat.getDokument();
                if (!personhistorikk.statsborgerskapListe.isEmpty()) {
                    statsborgerskap = personhistorikk.statsborgerskapListe.get(0).statsborgerskap;
                }
                break;
            }
        }
        if (statsborgerskap == null) {
            log.error("Funksjonell feil for prosessinstans {}: Kunne ikke hente brukers statsborgerskap fra saksopplysningene.", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Ingen informasjon om statsborgerskap", null);
            return;
        }

        // Kjør inngangsvilkår...
        List<String> søknadsland = prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class);
        søknadsland = tilIso3Landkoder(søknadsland);

        if (log.isDebugEnabled()) {
            log.debug("Kaller regelmodul for prosessinstans {}", prosessinstans.getId());
            log.debug("satsborgerskap: {}", statsborgerskap);
            log.debug("søknadsland: {}", String.join(" ", søknadsland));
            log.debug("periode: {}", periode);
        }
        VurderInngangsvilkaarReply res = regelmodulService.vurderInngangsvilkår(statsborgerskap, søknadsland, periode);

        // Legg på evt. feil og varsler...
        boolean detErMeldtFeil = false;
        for (Feilmelding melding : res.feilmeldinger) {
            if (melding.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL) {
                detErMeldtFeil = true;
            }
            log.info("Kall til regelmodul for prosessinstans {} returnerte {}", prosessinstans.getId(), melding);
            prosessinstans.leggTilHendelse(melding.kategori.name(), melding.melding);
        }

        // Håndter ev. feil...
        if (detErMeldtFeil) {
            log.info("Avbryter behandling av prosessinstans {} pga. feil", prosessinstans.getId());
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Uventet feil fra regelmodulen", null);
            return;
        }

        // Sett sakstype...
        Fagsak fagsak = behandling.getFagsak();
        Sakstyper nyFagsakstype = res.kvalifisererForEf883_2004 ? Sakstyper.EU_EOS : Sakstyper.FTRL; // Fikses når inngangsvilkårsvurdering også kvalifiserer for avtaler.
        if (fagsak.getType() != null && fagsak.getType() != nyFagsakstype) {
            log.error("Avbryter behandling av prosessinstans {}: Forsøk på å endre fagsakType fra {} til {}", prosessinstans.getId(), fagsak.getType(), nyFagsakstype);
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Forsøk på å endre fagsakType fra " + fagsak.getType() + " til " + nyFagsakstype, null);
            return;
        }
        fagsak.setType(nyFagsakstype);
        fagsakRepository.save(fagsak);

        prosessinstans.setSteg(ProsessSteg.HENT_ARBF_OPPL);
        log.info("Satt type på fagsak {} til {} for prosessinstans {}", fagsak.getSaksnummer(), nyFagsakstype, prosessinstans.getId());
    }

    private static List<String> tilIso3Landkoder(List<String> land) throws TekniskException {
        List<String> landkoder = new ArrayList<>();

        for (String l : land) {
            landkoder.add(LandkoderUtils.tilIso3(l));
        }
        return landkoder;
    }
}
