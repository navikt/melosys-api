package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.brev.FastMottaker;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.saksflyt.brev.FastMottaker.HELFO;
import static no.nav.melosys.saksflyt.brev.FastMottaker.SKATT;


/**
 * Sender ulike brev basert på behandlingsresultat og lovvalgsbestemmelse.
 */
@Component
public class SendVedtaksbrevInnland extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrevInnland.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public SendVedtaksbrevInnland(BrevBestiller brevBestiller,
                                  BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_SEND_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());
        // Henter ut behandling med saksopplysninger
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        Behandlingsresultattyper behandlingsresultatType = resultat.getType();
        String saksbehandler = hentSaksbehandler(prosessinstans, resultat);

        if (resultat.erAvslag()) {
            sendAvslagsbrev(behandling, behandlingsresultatType, saksbehandler);
            log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
        } else if (resultat.erInnvilgelse()) {
            sendInnvilgelsesbrev(prosessinstans, behandling, resultat, saksbehandler);
            log.info("Sendt innvilgelsesbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_SEND_SED);
        } else {
            log.warn("Innvilgelsesbrev kan ikke sendes for behandling {} i "
                    + "prosessinstansen {}.",
                behandling.getId(), prosessinstans.getId());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    private void sendAvslagsbrev(Behandling behandling, Behandlingsresultattyper behandlingsresultatType, String saksbehandler)
        throws FunksjonellException, TekniskException {
        Produserbaredokumenter avslagType = (behandlingsresultatType != Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            ? AVSLAG_YRKESAKTIV : AVSLAG_MANGLENDE_OPPLYSNINGER;

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(avslagType)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medMottakere(Mottaker.av(BRUKER), FastMottaker.av(HELFO), FastMottaker.av(SKATT))
            .build();
        brevBestiller.bestill(brevbestilling);

        if (behandling.getFagsak().harAktørMedRolleType(ARBEIDSGIVER)) {
            if (avslagType == AVSLAG_MANGLENDE_OPPLYSNINGER) {
                brevBestiller.bestill(AVSLAG_MANGLENDE_OPPLYSNINGER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
            } else {
                brevBestiller.bestill(AVSLAG_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
            }
        }
    }

    private void sendInnvilgelsesbrev(Prosessinstans prosessinstans, Behandling behandling, Behandlingsresultat resultat, String saksbehandler)
        throws FunksjonellException, TekniskException {
        Endretperiode endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);
        String begrunnelseKode = null;
        if (endretPeriodeBegrunnelseKode != null) {
            begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }

        Produserbaredokumenter innvilgelseType = (resultat.erInnvilgelseFlereLand())
            ? INNVILGELSE_YRKESAKTIV_FLERE_LAND : INNVILGELSE_YRKESAKTIV;

        Brevbestilling innvilgelseBrukerOgSkatt = new Brevbestilling.Builder().medDokumentType(innvilgelseType)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode)
            .medMottakere(Mottaker.av(BRUKER), FastMottaker.av(SKATT))
            .build();
        brevBestiller.bestill(innvilgelseBrukerOgSkatt);

        Fagsak fagsak = behandling.getFagsak();
        if (fagsak.harAktørMedRolleType(ARBEIDSGIVER)) {
            brevBestiller.bestill(INNVILGELSE_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
        }

        Brevbestilling A1_Myndighet = new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
            .medAvsender(saksbehandler)
            .medMottakere(Mottaker.av(MYNDIGHET))
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode).build();
        brevBestiller.bestill(A1_Myndighet);
    }

    private String hentSaksbehandler(Prosessinstans prosessinstans, Behandlingsresultat behandlingsresultat) {
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandler) && behandlingsresultat.erAutomatisert()) {
            saksbehandler = prosessinstans.getBehandling().getFagsak().getRegistrertAv();
        }
        return saksbehandler;
    }
}