package no.nav.melosys.saksflyt.steg.jfr;

import java.util.List;

import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPRETT_SØKNAD;

@Component
public class OpprettSoeknad implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettSoeknad.class);

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Autowired
    public OpprettSoeknad(BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException {
        long behandlingID = prosessinstans.getBehandling().getId();

        if (prosessinstans.getBehandling().erBehandlingAvSøknad()) {

            Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
            SoeknadDokument soeknadDokument = new SoeknadDokument();
            soeknadDokument.periode = periode;
            soeknadDokument.soeknadsland.landkoder = prosessinstans.getData(ProsessDataKey.SØKNADSLAND, List.class);

            behandlingsgrunnlagService.opprettSøknadGrunnlag(prosessinstans.getBehandling().getId(), soeknadDokument);
            log.info("Opprettet søknad for behandling {}.", prosessinstans.getId(), behandlingID);
        } else {
            log.info("Ikke opprettet søknad for behandling {} med tema {}", behandlingID, prosessinstans.getBehandling().getTema());
        }
    }
}
