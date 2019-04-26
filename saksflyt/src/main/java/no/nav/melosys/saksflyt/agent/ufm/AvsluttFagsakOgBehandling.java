package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandlingFelles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("avsluttFagsakOgBehandlingUnntakFraMedlemskap")
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final OppdaterFagsakOgBehandlingFelles felles;

    @Autowired
    public AvsluttFagsakOgBehandling(OppdaterFagsakOgBehandlingFelles felles) {
        this.felles = felles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        felles.oppdaterFagsakOgBehandlingStatuser(behandling, Saksstatuser.LOVVALG_AVKLART, Behandlingsstatus.AVSLUTTET);
        log.info("Periode suksessfult regisrert for fagsak {},behandling {}", behandling.getFagsak().getSaksnummer(), behandling.getId());
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
