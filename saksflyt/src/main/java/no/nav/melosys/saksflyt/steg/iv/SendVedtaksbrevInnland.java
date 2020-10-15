package no.nav.melosys.saksflyt.steg.iv;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
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
import no.nav.melosys.saksflyt.steg.StegBehandler;
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
public class SendVedtaksbrevInnland implements StegBehandler {
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
        String begrunnelseKode = hentBegrunnelseKode(prosessinstans);
        String fritekst = hentBegrunnelseFritekst(prosessinstans);

        if (resultat.erAvslag()) {
            sendAvslagsbrev(behandling, behandlingsresultatType, saksbehandler, fritekst);
            log.info("Sendt avslagsbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_OPPDATER_RESULTAT);
        } else if (resultat.erUtpeking()) {
            sendUtpekingsbrev(behandling, saksbehandler, fritekst);
            log.info("Sendt utpekingsbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_SEND_SED);
        } else if (resultat.erInnvilgelse()) {
            sendInnvilgelsesbrev(behandling, resultat, saksbehandler, begrunnelseKode, fritekst);
            sendOrienteringTilArbeidsgiver(behandling, resultat, saksbehandler);
            log.info("Sendt innvilgelsesbrev for prosessinstans {}", prosessinstans.getId());
            prosessinstans.setSteg(IV_SEND_SED);
        } else {
            throw new FunksjonellException("Vedtaksbrev kan ikke sendes for behandling " + behandling.getId());
        }
    }

    private void sendAvslagsbrev(Behandling behandling,
                                 Behandlingsresultattyper behandlingsresultatType,
                                 String saksbehandler,
                                 String fritekst)
        throws FunksjonellException, TekniskException {
        Produserbaredokumenter avslagTypeBruker = (behandlingsresultatType != Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            ? AVSLAG_YRKESAKTIV : AVSLAG_MANGLENDE_OPPLYSNINGER;

        List<Mottaker> mottakerListe;
        if (avslagTypeBruker == AVSLAG_YRKESAKTIV) {
            mottakerListe = List.of(Mottaker.av(BRUKER), av(HELFO), av(SKATT));
        } else {
            mottakerListe = List.of(Mottaker.av(BRUKER));
        }

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

    private void sendInnvilgelsesbrev(Behandling behandling,
                                      Behandlingsresultat resultat,
                                      String saksbehandler,
                                      String begrunnelseKode,
                                      String fritekst)
        throws FunksjonellException, TekniskException {
        Produserbaredokumenter innvilgelseType = (resultat.erInnvilgelseFlereLand())
            ? INNVILGELSE_YRKESAKTIV_FLERE_LAND : INNVILGELSE_YRKESAKTIV;

        List<Mottaker> mottakerListe = new ArrayList<>(List.of(Mottaker.av(BRUKER), FastMottaker.av(SKATT)));
        if (skalSendesTilSkatteoppkreverUtland(behandling.getBehandlingsgrunnlag())) {
            mottakerListe.add(FastMottaker.av(SKATTEOPPKREVER_UTLAND));
        }

        Brevbestilling innvilgelseBrukerOgSkatt = new Brevbestilling.Builder().medDokumentType(innvilgelseType)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode)
            .medMottakere(mottakerListe)
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(innvilgelseBrukerOgSkatt);
    }

    private void sendUtpekingsbrev(Behandling behandling, String saksbehandler, String fritekst)
        throws FunksjonellException, TekniskException {
        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(ORIENTERING_UTPEKING_UTLAND)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medMottakere(Mottaker.av(BRUKER))
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(brevbestilling);
    }

    private void sendOrienteringTilArbeidsgiver(Behandling behandling, Behandlingsresultat resultat, String saksbehandler)
        throws FunksjonellException, TekniskException {
        final Lovvalgsperiode lovvalgsperiode = resultat.hentValidertLovvalgsperiode();
        // Saker med kun selvstendig næringsdrivende skal ikke sende brevet INNVILGESE_ARBEIDSGIVER
        if (behandling.getFagsak().harAktørMedRolleType(ARBEIDSGIVER)
            && !lovvalgsperiode.erArtikkel13()
            && !lovvalgsperiode.erArtikkel11_4()) {
            brevBestiller.bestill(INNVILGELSE_ARBEIDSGIVER, saksbehandler, Mottaker.av(ARBEIDSGIVER), behandling);
        }
    }

    private static boolean skalSendesTilSkatteoppkreverUtland(Behandlingsgrunnlag behandlingsgrunnlag) {
        return finnesForetakUtland(behandlingsgrunnlag)
            && !finnesUtenlandskSelvstendigForetak(behandlingsgrunnlag);
    }

    private static boolean finnesForetakUtland(Behandlingsgrunnlag behandlingsgrunnlag) {
        return !behandlingsgrunnlag.getBehandlingsgrunnlagdata().foretakUtland.isEmpty();
    }

    private static boolean finnesUtenlandskSelvstendigForetak(Behandlingsgrunnlag behandlingsgrunnlag) {
        return behandlingsgrunnlag.getBehandlingsgrunnlagdata().foretakUtland.stream()
            .anyMatch(foretakUtland -> Boolean.TRUE.equals(foretakUtland.selvstendigNæringsvirksomhet));
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