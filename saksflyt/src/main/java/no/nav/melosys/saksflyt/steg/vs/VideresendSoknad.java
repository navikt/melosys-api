package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_STATUS_BEH_AVSL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.VS_SEND_SOKNAD;

/**
 * Sender et brev med søknad som vedlegg til utenlandsk myndighet
 *
 * Transisjoner:
 * VS_SEND_SOKNAD -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class VideresendSoknad extends AbstraktSendUtland {

    private static final Logger log = LoggerFactory.getLogger(VideresendSoknad.class);

    private final JoarkFasade joarkFasade;

    @Autowired
    protected VideresendSoknad(EessiService eessiService, BrevBestiller brevBestiller,
                               BehandlingsresultatService behandlingsresultatService,
                               LandvelgerService landvelgerService, @Qualifier("system") JoarkFasade joarkFasade) {
        super(eessiService, brevBestiller, behandlingsresultatService, landvelgerService);
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_SEND_SOKNAD;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        sendUtland(BucType.LA_BUC_03, prosessinstans, hentSøknadDokument(prosessinstans.getBehandling()));
        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }

    private byte[] hentSøknadDokument(Behandling behandling) throws FunksjonellException {
        String journalpostID = behandling.getInitierendeJournalpostId();
        String dokumentID = behandling.getInitierendeDokumentId();

        if (StringUtils.isEmpty(journalpostID)) {
            throw new FunksjonellException("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        } else if (StringUtils.isEmpty(dokumentID)) {
            throw new FunksjonellException("DokumentID til behandling " + behandling.getId() + " finnes ikke!");
        }

        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }

    @Override
    protected Brevbestilling lagBrevBestilling(Prosessinstans prosessinstans) {
        throw new UnsupportedOperationException("Videresending av søknad er ikke implementert!");
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return true;
    }
}
