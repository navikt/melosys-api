package no.nav.melosys.saksflyt.steg.jfr;

import java.util.List;

import no.nav.melosys.domain.dokument.soeknad.Periode;
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
        fagsakService.oppdaterType(prosessinstans.getData(ProsessDataKey.SAKSNUMMER), res.kvalifisererForEf883_2004);

        prosessinstans.setSteg(ProsessSteg.HENT_ARBF_OPPL);
    }
}
