package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.DokumentType;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.Dokumenttype.ATTEST_A1;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.GSAK_AVSLUTT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;

/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_SEND_BREV -> GSAK_AVSLUTT_OPPGAVE eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakSendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendBrev.class);

    private final DokumentSystemService dokumentService;

    private final BrevDataByggerVelger brevDataByggerVelger;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public IverksettVedtakSendBrev(DokumentSystemService dokumentService, BrevDataByggerVelger brevDataByggerVelger, BehandlingRepository behandlingRepository) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.behandlingRepository = behandlingRepository;

        log.info("IverksetteVedtakSendBrev initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_SEND_BREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        // Henter ut behandling på nytt for å få med saksopplysninger
        Behandling behandling = behandlingRepository.findOneWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException("Finner ikke behandling");
        }

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        ProsessType prosessType = prosessinstans.getType();
        if (ProsessType.IVERKSETT_VEDTAK == prosessType) {
            BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(DokumentType.ATTEST_A1);
            BrevData brevData = brevDataBygger.lag(behandling, saksbehandler);

            brevData.mottaker = RolleType.BRUKER;
            dokumentService.produserDokument(behandling.getId(), ATTEST_A1, brevData);

            brevData.mottaker = RolleType.ARBEIDSGIVER;
            dokumentService.produserDokument(behandling.getId(), ATTEST_A1, brevData);

            prosessinstans.setSteg(GSAK_AVSLUTT_OPPGAVE);
        } else {
            String feilmelding = "Ukjent prosess type: " + prosessType;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
        }
    }
}
