package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvsluttTidligerePeriode extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttTidligerePeriode.class);

    private final OppdaterMedlFelles felles;
    private final MedlFasade medlFasade;

    @Autowired
    public AvsluttTidligerePeriode(OppdaterMedlFelles felles, MedlFasade medlFasade) {
        this.felles = felles;
        this.medlFasade = medlFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (prosessinstans.getData(ProsessDataKey.ER_ENDRING, Boolean.class)) {
            avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT);
    }

    private void avsluttTidligerMedlPeriode(Fagsak fagsak) throws FunksjonellException {
        Behandling tidligereBehandling = fagsak.getTidligsteInaktiveBehandling();

        if (tidligereBehandling != null) {
            log.info("Avslutter tidligere periode for fagsak {}", fagsak.getSaksnummer());
            Lovvalgsperiode lovvalgsperiode = felles.hentLovvalgsperiode(tidligereBehandling);
            medlFasade.avvisPeriode(lovvalgsperiode, StatusaarsakMedl.AVVIST);
        }
    }
}
