package no.nav.melosys.saksflyt.steg.vs;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.VS_VIDERESEND_SAK;
import static no.nav.melosys.domain.ProsessSteg.VS_SEND_BREV;

/**
 * Videresender Sak
 *
 * Transisjoner:
 * VS_VIDERESEND_SAK -> VS_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component()
public class VideresendSak extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(VideresendSak.class);

    private final OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling;

    @Autowired
    public VideresendSak(OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling) {
        this.oppdaterFagsakOgBehandling = oppdaterFagsakOgBehandling;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_VIDERESEND_SAK;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        oppdaterFagsakOgBehandling.oppdaterFagsakOgBehandlingStatuser(behandling, Saksstatuser.VIDERESENDT, Behandlingsstatus.AVSLUTTET);

        log.info("Satt sak til videresendt for prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(VS_SEND_BREV);
    }
}
