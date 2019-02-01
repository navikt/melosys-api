package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAnmodningUnntakOgAvslag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.AVSLAG_YRKESAKTIV;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV;
import static no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator.avslagsbrevSkalSendes;
import static no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator.validerLovvalgsperiode;

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

    private final DokumentSystemService dokumentService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsResultatRepo;

    @Autowired
    public IverksettVedtakSendBrev(DokumentSystemService dokumentService,
            BrevDataByggerVelger brevDataByggerVelger,
            BehandlingRepository behandlingRepository,
            BehandlingsresultatRepository behandlingsResultatRepo) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
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
        // Henter ut behandling på nytt for å få med saksopplysninger
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
        if (ProsessType.IVERKSETT_VEDTAK == prosessType) {
            String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

            if (avslagsbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
                BrevDataByggerAnmodningUnntakOgAvslag brevDataBygger = (BrevDataByggerAnmodningUnntakOgAvslag) brevDataByggerVelger.hent(AVSLAG_YRKESAKTIV);
                BrevData brevData = brevDataBygger.lag(behandling, saksbehandler);
                dokumentService.produserDokument(behandling.getId(), AVSLAG_YRKESAKTIV, brevData);

                log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
                prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
            } else if (SendBrevValidator.innvilgelsesbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
                BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(INNVILGELSE_YRKESAKTIV);
                BrevData brevData = brevDataBygger.lag(behandling, saksbehandler);
                brevData.mottaker = Aktoersroller.BRUKER;
                dokumentService.produserDokument(behandling.getId(), INNVILGELSE_YRKESAKTIV, brevData);

                // FIXME Myndigheter støttes ikke.
                //brevData.mottaker = RolleType.MYNDIGHET;
                //dokumentService.produserDokument(behandling.getId(), ATTEST_A1, brevData);

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

    /**
     * Finn ut om innvilgelsesbrev skal sendes.
     * 
     * Innvilgelsesbrev skal sendes dersom behandlingen har resultert i:
     * <ul>
     * <li>Lovvalgsland er avklart</li>
     * <li>Lovvalgsland er Norge</li>
     * <li>Lovvalgbestemmelsen er 12.1, 12.2 eller 16.1</li>
     * 
     * </ul>
     * 
     * @param behandling
     *            en behandling å sjekke.
     * @return <code>true</code> hvis innvilgelsesbrev skal sendes, ellers <code>false</code>.
     */
    private boolean innvilgelsesbrevSkalSendes(Behandling behandling) {
        Behandlingsresultat resultat = behandlingsResultatRepo.findById(behandling.getId()).orElse(null);
        Set<Lovvalgsperiode> lovvalgsperioder = resultat.getLovvalgsperioder();
        if (lovvalgsperioder.size() > 1) {
            throw new UnsupportedOperationException(String.format("Flere enn en"
                    + " lovvalgsperiode er ikke støttet i første leveranse av "
                    + "Melosys (behandlingsid %s).", behandling.getId()));
        }
        LovvalgBestemmelse bestemmelse = resultat.getLovvalgsperioder().iterator().next().getBestemmelse();
        return resultat.getType() == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND &&
                resultat.getFastsattAvLand() == Landkoder.NO &&
                bestemmelseKanInnvilges(bestemmelse);
    }

    private static boolean bestemmelseKanInnvilges(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1 ||
                bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2 ||
                bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1;
    }
}
