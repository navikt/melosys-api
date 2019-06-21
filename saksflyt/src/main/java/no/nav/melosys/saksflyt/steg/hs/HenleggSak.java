package no.nav.melosys.saksflyt.steg.hs;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.HS_HENLEGG_SAK;
import static no.nav.melosys.domain.ProsessSteg.HS_SEND_BREV;

/**
 * Henlegger Sak
 *
 * Transisjoner:
 * HS_HENLEGG_SAK -> HS_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class HenleggSak extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HenleggSak.class);

    private final OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling;

    @Autowired
    public HenleggSak(OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling) {
        this.oppdaterFagsakOgBehandling = oppdaterFagsakOgBehandling;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HS_HENLEGG_SAK;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        oppdaterFagsakOgBehandling.oppdaterFagsakOgBehandlingStatuser(behandling, Saksstatuser.HENLAGT, Behandlingsstatus.AVSLUTTET);

        log.info("Satt sak til henlagt for prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(HS_SEND_BREV);
    }
}
