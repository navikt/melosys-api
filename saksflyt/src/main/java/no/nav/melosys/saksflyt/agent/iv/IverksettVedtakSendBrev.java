package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.BrevBestiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator.*;


/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_SEND_BREV -> IV_AVSLUTT_BEHANDLING eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakSendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendBrev.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsResultatRepo;

    @Autowired
    public IverksettVedtakSendBrev(BrevBestiller brevBestiller,
            BehandlingRepository behandlingRepository,
            BehandlingsresultatRepository behandlingsResultatRepo) {
        this.brevBestiller = brevBestiller;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsResultatRepo = behandlingsResultatRepo;

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
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());
        // Henter ut behandling med saksopplysninger
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }

        Behandlingsresultat resultat = behandlingsResultatRepo.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat " + behandling.getId()));
        Behandlingsresultattyper behandlingsresultatType = resultat.getType();
        Lovvalgsperiode lovvalgsperiode = validerLovvalgsperiode(resultat.getLovvalgsperioder());
        log.info("Behandler lovvalgsperiode: {}", lovvalgsperiode);

        ProsessType prosessType = prosessinstans.getType();
        if (ProsessType.IVERKSETT_VEDTAK == prosessType || ProsessType.IVERKSETT_VEDTAK_ENDRET_PERIODE == prosessType) {
            String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

            if (avslagsbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
                brevBestiller.bestill(behandling, saksbehandler, AVSLAG_YRKESAKTIV, BRUKER);
                // FIXME Støtte for arbeidsgivere mangler.
                //brevBestiller.bestill(behandling, saksbehandler, AVSLAG_ARBEIDSGIVER, ARBEIDSGIVER);

                log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
                prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
            } else if (innvilgelsesbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
                Endretperioder endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.class);
                brevBestiller.bestill(behandling, saksbehandler, INNVILGELSE_YRKESAKTIV, BRUKER, endretPeriodeBegrunnelseKode);
                // FIXME Støtte for arbeidsgivere mangler.
                //brevBestiller.bestill(behandling, saksbehandler, INNVILGELSE_ARBEIDSGIVER, ARBEIDSGIVER, endretPeriodeBegrunnelseKode);
                brevBestiller.bestill(behandling, saksbehandler, ATTEST_A1, MYNDIGHET, endretPeriodeBegrunnelseKode);

                log.info("Sendt innvilgelsesbrev for prosessinstans {}", prosessinstans.getId());
                prosessinstans.setSteg(IV_SEND_SED);
            } else {
                log.warn("Innvilgelsesbrev kan ikke sendes for behandling {} i "
                        + "prosessinstansen {}.",
                        behandling.getId(), prosessinstans.getId());
                prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            }
        } else {
            String feilmelding = "Ukjent prosess type: " + prosessType;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
        }
    }
}
