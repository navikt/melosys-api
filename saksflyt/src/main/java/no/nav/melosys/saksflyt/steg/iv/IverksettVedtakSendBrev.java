package no.nav.melosys.saksflyt.steg.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.brev.FastMottaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static no.nav.melosys.saksflyt.steg.iv.validering.SendBrevValidator.*;
import static no.nav.melosys.saksflyt.brev.FastMottaker.HELFO;
import static no.nav.melosys.saksflyt.brev.FastMottaker.SKATT;


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
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public IverksettVedtakSendBrev(BrevBestiller brevBestiller,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingsresultatRepository behandlingsResultatRepo,
                                   UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.brevBestiller = brevBestiller;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsResultatRepo = behandlingsResultatRepo;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;

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

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        Fagsak fagsak = behandling.getFagsak();

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType == ProsessType.IVERKSETT_VEDTAK_AVSLAG_MANGLENDE_OPPLYSNINGER) {
            sendAvslagsbrevManglendeOpplysninger(behandling, saksbehandler, fagsak);
            prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
            return;
        }

        Behandlingsresultat resultat = behandlingsResultatRepo.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat " + behandling.getId()));
        Behandlingsresultattyper behandlingsresultatType = resultat.getType();
        Lovvalgsperiode lovvalgsperiode = validerLovvalgsperiode(resultat.getLovvalgsperioder());
        log.info("Behandler lovvalgsperiode: {}", lovvalgsperiode);

        if (avslagsbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
            sendAvslagsbrev(behandling, saksbehandler, fagsak);
            log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
        } else if (innvilgelsesbrevSkalSendes(behandlingsresultatType, lovvalgsperiode)) {
            sendInnvilgelsesbrev(prosessinstans, behandling, saksbehandler, fagsak);
            log.info("Sendt innvilgelsesbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_SEND_SED);
        } else {
            log.warn("Innvilgelsesbrev kan ikke sendes for behandling {} i "
                    + "prosessinstansen {}.",
                behandling.getId(), prosessinstans.getId());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    private void sendAvslagsbrevManglendeOpplysninger(Behandling behandling, String saksbehandler, Fagsak fagsak) throws FunksjonellException, TekniskException {
        brevBestiller.bestill(AVSLAG_MANGLENDE_OPPLYSNINGER, saksbehandler, Mottaker.av(BRUKER), behandling);
        if (fagsak.harAktørMedRolleType(ARBEIDSGIVER)) {
            brevBestiller.bestill(AVSLAG_MANGLENDE_OPPLYSNINGER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
        }
    }

    private void sendAvslagsbrev(Behandling behandling, String saksbehandler, Fagsak fagsak) throws FunksjonellException, TekniskException {
        brevBestiller.bestill(AVSLAG_YRKESAKTIV, saksbehandler, Mottaker.av(BRUKER), behandling);

        if (fagsak.harAktørMedRolleType(ARBEIDSGIVER)) {
            brevBestiller.bestill(AVSLAG_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
        }

        brevBestiller.bestill(AVSLAG_YRKESAKTIV, saksbehandler, FastMottaker.av(HELFO), behandling);
        brevBestiller.bestill(AVSLAG_YRKESAKTIV, saksbehandler, FastMottaker.av(SKATT), behandling);
    }

    private void sendInnvilgelsesbrev(Prosessinstans prosessinstans, Behandling behandling, String saksbehandler, Fagsak fagsak) throws FunksjonellException, TekniskException {
        Endretperioder endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.class);
        String begrunnelseKode = null;
        if (endretPeriodeBegrunnelseKode != null) {
            begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }

        Brevbestilling.Builder innvilgelseBuilder = new Brevbestilling.Builder().medDokumentType(INNVILGELSE_YRKESAKTIV)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode);
        Brevbestilling innvilgelseBruker = innvilgelseBuilder.medMottaker(Mottaker.av(BRUKER)).build();
        brevBestiller.bestill(innvilgelseBruker);
        Brevbestilling innvilgelseSkatt = innvilgelseBuilder.medMottaker(FastMottaker.av(SKATT)).build();
        brevBestiller.bestill(innvilgelseSkatt);

        if (fagsak.harAktørMedRolleType(ARBEIDSGIVER)) {
            brevBestiller.bestill(INNVILGELSE_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
        }
        if (myndighetØnskerInnvilgelsesbrev(fagsak.hentMyndighetLandkode())) {
            Brevbestilling A1_Myndighet = new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
                .medAvsender(saksbehandler)
                .medMottaker(Mottaker.av(MYNDIGHET))
                .medBehandling(behandling)
                .medBegrunnelseKode(begrunnelseKode).build();
            brevBestiller.bestill(A1_Myndighet);
        }
    }

    private boolean myndighetØnskerInnvilgelsesbrev(Landkoder landkode) throws TekniskException {
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."))
            .preferanser.stream().map(Preferanse::getPreferanse)
            .noneMatch(p -> p.equals(Preferanse.PreferanseEnum.RESERVERT_FRA_A1));
    }
}
