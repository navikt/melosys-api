package no.nav.melosys.saksflyt.steg.jfr;

import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflyt.feil.Feilkategori.FUNKSJONELL_FEIL;

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
    private final FagsakService fagsakService;

    @Autowired
    public VurderInngangsvilkaar(RegelmodulService regelmodulService,
                                 FagsakService fagsakService) {
        this.regelmodulService = regelmodulService;
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_VURDER_INNGANGSVILKÅR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        long behandlingID = prosessinstans.getBehandling().getId();

        // Kjør inngangsvilkår...
        List<String> søknadsland = prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);

        VurderInngangsvilkaarReply res = regelmodulService.vurderInngangsvilkår(behandlingID, søknadsland, periode);

        // Sett sakstype...
        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getData(ProsessDataKey.SAKSNUMMER));
        Sakstyper nyFagsakstype;
        if (Boolean.TRUE.equals(res.kvalifisererForEf883_2004)) {
            nyFagsakstype = Sakstyper.EU_EOS;
        } else {
            nyFagsakstype = Sakstyper.UKJENT;
        }
        if (fagsak.getType() != null && fagsak.getType() != nyFagsakstype && Sakstyper.UKJENT != fagsak.getType()) {
            log.error("Avbryter behandling av prosessinstans {}: Forsøk på å endre fagsakType fra {} til {}", prosessinstans.getId(), fagsak.getType(), nyFagsakstype);
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "Forsøk på å endre fagsakType fra " + fagsak.getType() + " til " + nyFagsakstype, null);
            return;
        }
        fagsak.setType(nyFagsakstype);
        fagsakService.lagre(fagsak);

        prosessinstans.setSteg(ProsessSteg.HENT_ARBF_OPPL);
        log.info("Satt type på fagsak {} til {} for prosessinstans {}", fagsak.getSaksnummer(), nyFagsakstype, prosessinstans.getId());
    }
}
