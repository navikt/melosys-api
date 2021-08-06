package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
    public void utfør(Prosessinstans prosessinstans) {
        long behandlingID = prosessinstans.getBehandling().getId();

        if (prosessinstans.getBehandling().erBehandlingAvSøknad()) {

            Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
            Soeknad soeknad = new Soeknad();
            soeknad.periode = periode;
            soeknad.soeknadsland.landkoder = prosessinstans.getData(ProsessDataKey.SØKNADSLAND, new TypeReference<List<String>>() {});
            Sakstyper sakstype = prosessinstans.getBehandling().getFagsak().getType();
            if (sakstype == Sakstyper.FTRL || sakstype == Sakstyper.TRYGDEAVTALE) {
                behandlingsgrunnlagService.opprettSøknadFolketrygden(behandlingID, new SoeknadFtrl());
            } else {
                behandlingsgrunnlagService.opprettSøknadYrkesaktiveEøs(behandlingID, soeknad);
            }
            log.info("Opprettet søknad for behandling {}.", behandlingID);
        } else {
            log.info("Ikke opprettet søknad for behandling {} med tema {}", behandlingID, prosessinstans.getBehandling().getTema());
        }
    }
}
