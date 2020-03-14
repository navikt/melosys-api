package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.brev.FastMottaker;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;
import static no.nav.melosys.saksflyt.brev.FastMottaker.*;


/**
 * Sender ulike brev basert på behandlingsresultat og lovvalgsbestemmelse.
 */
@Component
public class SendVedtaksbrevInnland extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrevInnland.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    @Autowired
    public SendVedtaksbrevInnland(BrevBestiller brevBestiller,
                                  BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService,
                                  AvklarteVirksomheterService avklarteVirksomheterService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
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
            sendAvslagsbrev(prosessinstans, behandling, behandlingsresultatType, saksbehandler);
            log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_AVSLUTT_BEHANDLING);
        } else if (resultat.erInnvilgelse()) {
            sendInnvilgelsesbrev(prosessinstans, behandling, resultat, saksbehandler);
            log.info("Sendt innvilgelsesbrev for prosessinstans {}", prosessinstans.getId());
            if (behandling.norgeErUtpekt()) {
                prosessinstans.setSteg(IV_SEND_GODKJENT_UTPEKING);
            } else {
                prosessinstans.setSteg(IV_SEND_SED);
            }
        } else {
            log.warn("Vedtaksbrev kan ikke sendes for behandling {} i "
                    + "prosessinstansen {}.",
                behandling.getId(), prosessinstans.getId());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    private void sendAvslagsbrev(Prosessinstans prosessinstans, Behandling behandling, Behandlingsresultattyper behandlingsresultatType, String saksbehandler)
        throws FunksjonellException, TekniskException {
        Produserbaredokumenter avslagTypeBruker = (behandlingsresultatType != Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            ? AVSLAG_YRKESAKTIV : AVSLAG_MANGLENDE_OPPLYSNINGER;

        List<Mottaker> mottakerListe;
        if (avslagTypeBruker == AVSLAG_YRKESAKTIV) {
            mottakerListe = List.of(Mottaker.av(BRUKER), av(HELFO), av(SKATT));
        } else {
            mottakerListe = List.of(Mottaker.av(BRUKER));
        }

        String fritekst = hentBegrunnelseFritekst(prosessinstans);

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(avslagTypeBruker)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medMottakere(mottakerListe)
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(brevbestilling);

        if (behandling.getFagsak().harAktørMedRolleType(ARBEIDSGIVER)) {
            Produserbaredokumenter avslagTypeArbeidsgiver = (behandlingsresultatType != Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
                ? AVSLAG_ARBEIDSGIVER : AVSLAG_MANGLENDE_OPPLYSNINGER;

            Brevbestilling.Builder brevbestillingArbeidsgiver = new Brevbestilling.Builder()
                .medDokumentType(avslagTypeArbeidsgiver)
                .medAvsender(saksbehandler)
                .medBehandling(behandling)
                .medMottakere(Mottaker.av(ARBEIDSGIVER))
                .medFritekst(fritekst);

            brevBestiller.bestill(brevbestillingArbeidsgiver.build());
        }
    }

    private void sendInnvilgelsesbrev(Prosessinstans prosessinstans, Behandling behandling, Behandlingsresultat resultat, String saksbehandler)
        throws FunksjonellException, TekniskException {
        Produserbaredokumenter innvilgelseType = (resultat.erInnvilgelseFlereLand())
            ? INNVILGELSE_YRKESAKTIV_FLERE_LAND : INNVILGELSE_YRKESAKTIV;

        Brevbestilling innvilgelseBrukerOgSkatt = new Brevbestilling.Builder().medDokumentType(innvilgelseType)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans))
            .medMottakere(Mottaker.av(BRUKER), FastMottaker.av(SKATT))
            .medFritekst(hentBegrunnelseFritekst(prosessinstans))
            .build();
        brevBestiller.bestill(innvilgelseBrukerOgSkatt);


        final boolean erArtikkel13 = resultat.hentValidertLovvalgsperiode().erArtikkel13();
        if (erArtikkel13) {
            if (!behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().foretakUtland.isEmpty()) {
                Brevbestilling a1SkatteoppkreverUtland = new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
                    .medAvsender(saksbehandler)
                    .medMottakere(FastMottaker.av(SKATTEOPPKREVER_UTLAND))
                    .medBehandling(behandling)
                    .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans)).build();
                brevBestiller.bestill(a1SkatteoppkreverUtland);
            }
        } else {
            // Saker for art. 13 eller med kun selvstendig næringsdrivende skal ikke sende brevet INNVILGESE_ARBEIDSGIVER
            Fagsak fagsak = behandling.getFagsak();
            if (fagsak.harAktørMedRolleType(ARBEIDSGIVER)) {
                brevBestiller.bestill(INNVILGELSE_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
            }
            if (harValgteUtenlandskeVirksomheter(behandling)) {
                Brevbestilling a1SkatteoppkreverUtland = new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
                    .medAvsender(saksbehandler)
                    .medMottakere(FastMottaker.av(SKATTEOPPKREVER_UTLAND))
                    .medBehandling(behandling)
                    .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans)).build();
                brevBestiller.bestill(a1SkatteoppkreverUtland);
            }
        }
    }

    private boolean harValgteUtenlandskeVirksomheter(Behandling behandling) {
        return !avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).isEmpty();
    }

    private String hentBegrunnelseKode(Prosessinstans prosessinstans) {
        Endretperiode endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);
        String begrunnelseKode = null;
        if (endretPeriodeBegrunnelseKode != null) {
            begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }
        return begrunnelseKode;
    }

    private String hentBegrunnelseFritekst(Prosessinstans prosessinstans) {
        return prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST);
    }

    private String hentSaksbehandler(Prosessinstans prosessinstans, Behandlingsresultat behandlingsresultat) {
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandler) && behandlingsresultat.erAutomatisert()) {
            saksbehandler = prosessinstans.getBehandling().getFagsak().getRegistrertAv();
        }
        return saksbehandler;
    }
}