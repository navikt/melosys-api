// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;

/**
 * Ferdigstille behandling med oppdatere endringsdato på behandling og oppdatere prosessinstans steg ved oppfrisking
 *
 * Transisjoner:
 * OPPFRISK_SAKSOPPLYSNINGER -> null eller OPPRETT_OPPGAVE eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppfriskSaksopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysninger.class);

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public OppfriskSaksopplysninger(BehandlingRepository behandlingRepository) {
        log.info("OppfriskSaksopplysninger initialisert");
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPFRISK_SAKSOPPLYSNINGER;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        behandling.setSisteOpplysningerHentetDato(LocalDateTime.now());
        behandlingRepository.save(behandling);

        Boolean oppfriskSaksopplysning = prosessinstans.getData(ProsessDataKey.OPPFRISK_SAKSOPPLYSNING, Boolean.class);
        if (oppfriskSaksopplysning != null && oppfriskSaksopplysning) {
            prosessinstans.setSteg(null);
            log.info("Oppfrisking av saksopplysning er ferdig for prosessinstans {}", prosessinstans.getId());
            return;
        } else {
            prosessinstans.setSteg(OPPRETT_OPPGAVE);
        }
        log.info("Ferdigstilt Behandling {}", prosessinstans.getId());
    }
}
