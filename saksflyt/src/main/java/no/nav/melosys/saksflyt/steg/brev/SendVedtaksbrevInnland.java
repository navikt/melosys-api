package no.nav.melosys.saksflyt.steg.brev;

import java.util.ArrayList;
import java.util.List;

import io.getunleash.Unleash;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia.*;
import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.saksflytapi.domain.ProsessSteg.SEND_VEDTAKSBREV_INNLAND;

@Component
public class SendVedtaksbrevInnland implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrevInnland.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlingRegler saksbehandlingRegler;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    private final Unleash unleash;

    public SendVedtaksbrevInnland(BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService,
                                  ProsessinstansService prosessinstansService,
                                  SaksbehandlingRegler saksbehandlingRegler,
                                  AvklarteVirksomheterService avklarteVirksomheterService,
                                  Unleash unleash) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return SEND_VEDTAKSBREV_INNLAND;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(prosessinstans.getBehandling().getId());
        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        String saksbehandler = hentSaksbehandler(prosessinstans, resultat);
        String begrunnelseKode = hentBegrunnelsekodeTilForkortetPeriode(resultat);
        String fritekst = hentBegrunnelseFritekst(prosessinstans);

        final Lovvalgsperiode lovvalgsperiode = resultat.hentLovvalgsperiode();

        var virksomhet = resultat.getAvklartefakta().stream().filter(avklartfakta -> avklartfakta.getType() == Avklartefaktatyper.VIRKSOMHET)
            .map(Avklartefakta::getSubjekt)
            .findFirst()
            .orElse(null);

        boolean erSelvstendigVirksomhet = avklarteVirksomheterService.hentNorskeSelvstendigeForetak(behandling).stream()
            .anyMatch(selvstendigVirksomhet -> selvstendigVirksomhet.orgnr.equals(virksomhet));

        boolean harAvhuketSelvstendigNæringsdrivende = resultat.getAvklartefakta().stream()
            .filter(avklartfakta -> avklartfakta.getFakta().equals("SELVSTENDIG_NAERINGSDRIVENDE"))
            .findFirst()
            .orElse(null) != null;

        boolean erSelvstendig = erSelvstendigVirksomhet || harAvhuketSelvstendigNæringsdrivende;

        if (resultat.erAvslag()) {
            boolean erStorBrittanniaArt18 = KONV_EFTA_STORBRITANNIA_ART18_1 == lovvalgsperiode.getBestemmelse();
            sendAvslagsbrev(behandling, saksbehandler, fritekst, erStorBrittanniaArt18, prosessinstans.getData(ProsessDataKey.ARBEIDSGIVER_SKAL_HA_KOPI, Boolean.class, true), erSelvstendig);
            log.info("Sendt avslagsbrev for behandling {}", behandling.getId());
        } else if (resultat.erUtpeking()) {
            sendUtpekingsbrev(behandling, saksbehandler, fritekst);
            log.info("Sendt utpekingsbrev for behandling {}", behandling.getId());
        } else if (resultat.erInnvilgelse()) {
            boolean erStorbritanniaBestemmelse = lovvalgsperiode.erEftaStorbritannia();
            sendInnvilgelsesbrev(behandling, resultat, saksbehandler, begrunnelseKode, fritekst, erStorbritanniaBestemmelse);
            if (unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA) && erStorbritanniaBestemmelse) {
                sendAttestA1(behandling, saksbehandler, begrunnelseKode, fritekst);
            }
            log.info("Sendt innvilgelsesbrev for behandling {}", behandling.getId());
            if (prosessinstans.getData(ProsessDataKey.ARBEIDSGIVER_SKAL_HA_KOPI, Boolean.class, true)) {
                if (unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA)) {
                    if (!erSelvstendig) {
                        sendOrienteringTilArbeidsgiverEFTATogglet(behandling, resultat, saksbehandler);
                        log.info("Sendt orienteringsbrev til arbeidsgiver for behandling {}", behandling.getId());
                    }
                } else {
                    sendOrienteringTilArbeidsgiver(behandling, resultat, saksbehandler);
                    log.info("Sendt orienteringsbrev til arbeidsgiver for behandling {}", behandling.getId());
                }
            }
        } else {
            throw new FunksjonellException("Vedtaksbrev kan ikke sendes for behandling " + behandling.getId());
        }
    }

    private void sendAvslagsbrev(Behandling behandling, String saksbehandler, String fritekst, boolean erStorBrittanniaArt18, boolean arbeidsgiverSkalHaKopi, boolean erSelvstendig) {
        var mottakerListe = List.of(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.av(NorskMyndighet.HELFO), Mottaker.av(NorskMyndighet.SKATTEETATEN));

        var produserBartDokument = unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA) &&
            erStorBrittanniaArt18 ? AVSLAG_EFTA_STORBRITANNIA : AVSLAG_YRKESAKTIV;

        DoksysBrevbestilling brevbestilling = lagDoksysBrevbestilling(produserBartDokument, saksbehandler, fritekst);
        prosessinstansService.opprettProsessinstanserSendBrev(behandling, brevbestilling, mottakerListe);

        if (behandling.getFagsak().harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER) && arbeidsgiverSkalHaKopi && !erSelvstendig) {
            DoksysBrevbestilling brevbestillingArbeidsgiver = lagDoksysBrevbestilling(unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA) ? ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK : AVSLAG_ARBEIDSGIVER, saksbehandler, fritekst);
            prosessinstansService.opprettProsessinstansSendBrev(behandling, brevbestillingArbeidsgiver, Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER));
        }
    }

    private void sendInnvilgelsesbrev(Behandling behandling,
                                      Behandlingsresultat resultat,
                                      String saksbehandler,
                                      String begrunnelseKode,
                                      String fritekst,
                                      Boolean erStorbritannia) {

        Produserbaredokumenter produserbaredokument = hentProduserbarDokumentForInnvilgelse(behandling, resultat, erStorbritannia);

        List<Mottaker> mottakerListe = new ArrayList<>(List.of(Mottaker.medRolle(Mottakerroller.BRUKER)));
        if (brevSendesTilStatligSkatteoppkreving(
            resultat.hentLovvalgsperiode(),
            behandling.getMottatteOpplysninger()
        )) {
            mottakerListe.add(Mottaker.av(NorskMyndighet.SKATTEINNKREVER_UTLAND));
        }

        DoksysBrevbestilling innvilgelseBrukerOgSkatt = new DoksysBrevbestilling.Builder().medProduserbartDokument(produserbaredokument)
            .medAvsenderID(saksbehandler)
            .medBegrunnelseKode(begrunnelseKode)
            .medFritekst(fritekst)
            .build();
        prosessinstansService.opprettProsessinstanserSendBrev(behandling, innvilgelseBrukerOgSkatt, mottakerListe);
    }

    private void sendAttestA1(Behandling behandling,
                              String saksbehandler,
                              String begrunnelseKode,
                              String fritekst) {
        List<Mottaker> mottakerListe = new ArrayList<>(List.of(
            Mottaker.medRolle(Mottakerroller.BRUKER),
            Mottaker.medRolle(Mottakerroller.FULLMEKTIG)
        ));

        if (!avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).isEmpty()) {
            mottakerListe.add(Mottaker.av(NorskMyndighet.SKATTEINNKREVER_UTLAND));
        }

        DoksysBrevbestilling innvilgelseBrukerOgSkatt = new DoksysBrevbestilling.Builder().medProduserbartDokument(ATTEST_A1)
            .medAvsenderID(saksbehandler)
            .medBegrunnelseKode(begrunnelseKode)
            .medFritekst(fritekst)
            .build();
        prosessinstansService.opprettProsessinstanserSendBrev(behandling, innvilgelseBrukerOgSkatt, mottakerListe);
    }


    @NotNull
    private Produserbaredokumenter hentProduserbarDokumentForInnvilgelse(Behandling behandling, Behandlingsresultat resultat, boolean erStorbritanniaBestemmelse) {
        boolean erUtsendtArbeidstakerEllerSelvstendig = behandling.getTema() == Behandlingstema.UTSENDT_ARBEIDSTAKER || behandling.getTema() == Behandlingstema.UTSENDT_SELVSTENDIG || behandling.getTema() == Behandlingstema.ARBEID_KUN_NORGE;

        if ((erStorbritanniaBestemmelse && erUtsendtArbeidstakerEllerSelvstendig) && unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA)) {
            return INNVILGELSE_EFTA_STORBRITANNIA;
        }

        if (saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling.getFagsak().getType(), behandling.getTema())) {
            return IKKE_YRKESAKTIV_VEDTAKSBREV;
        }
        if (resultat.erInnvilgelseFlereLand()) {
            return INNVILGELSE_YRKESAKTIV_FLERE_LAND;
        }
        return INNVILGELSE_YRKESAKTIV;
    }

    private void sendUtpekingsbrev(Behandling behandling, String saksbehandler, String fritekst) {
        DoksysBrevbestilling brevbestilling = lagDoksysBrevbestilling(ORIENTERING_UTPEKING_UTLAND, saksbehandler, fritekst);
        prosessinstansService.opprettProsessinstansSendBrev(behandling, brevbestilling, Mottaker.medRolle(Mottakerroller.BRUKER));
    }

    private void sendOrienteringTilArbeidsgiver(Behandling behandling, Behandlingsresultat resultat, String saksbehandler) {
        final Lovvalgsperiode lovvalgsperiode = resultat.hentLovvalgsperiode();
        // Saker med kun selvstendig næringsdrivende skal ikke sende brevet INNVILGELSE_ARBEIDSGIVER
        if (behandling.getFagsak().harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)
            && !lovvalgsperiode.erArtikkel13()
            && !lovvalgsperiode.erArtikkel11_4()) {

            DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
                .medProduserbartDokument(INNVILGELSE_ARBEIDSGIVER)
                .medAvsenderID(saksbehandler)
                .build();
            prosessinstansService.opprettProsessinstansSendBrev(behandling, brevbestilling, Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER));
        }
    }

    private boolean harArtikkelRelevantForStatligSkatteoppkreving(Behandling behandling, Behandlingsresultat resultat) {
        final Lovvalgsperiode lovvalgsperiode = resultat.hentLovvalgsperiode();
        var bestemmelse = lovvalgsperiode.getBestemmelse();

        return behandling.getFagsak().harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)
            && !lovvalgsperiode.erArtikkel13()
            && !lovvalgsperiode.erArtikkel11_4()
            && bestemmelse != FO_883_2004_ART12_2
            && bestemmelse != KONV_EFTA_STORBRITANNIA_ART14_2
            && bestemmelse != KONV_EFTA_STORBRITANNIA_ART16_3;
    }

    private void sendOrienteringTilArbeidsgiverEFTATogglet(Behandling behandling, Behandlingsresultat resultat, String saksbehandler) {
        if (harArtikkelRelevantForStatligSkatteoppkreving(behandling, resultat)) {
            DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
                .medProduserbartDokument(ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK)
                .medAvsenderID(saksbehandler)
                .build();
            prosessinstansService.opprettProsessinstansSendBrev(behandling, brevbestilling, Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER));
        }
    }

    private static boolean brevSendesTilStatligSkatteoppkreving(Lovvalgsperiode lovvalgsperiode, MottatteOpplysninger mottatteOpplysninger) {
        return harArtikkelRelevantForStatligSkatteoppkreving(lovvalgsperiode)
            && finnesArbeidsgiverUtland(mottatteOpplysninger);
    }

    public static boolean harArtikkelRelevantForStatligSkatteoppkreving(Lovvalgsperiode lovvalgsperiode) {
        return lovvalgsperiode.erArtikkel11()
            || lovvalgsperiode.erArtikkel13_1()
            || lovvalgsperiode.getBestemmelse() == FO_883_2004_ART13_4
            || lovvalgsperiode.getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_4
            || lovvalgsperiode.getBestemmelse() == FO_883_2004_ART16_1
            || lovvalgsperiode.getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART18_1;
    }

    private static boolean finnesArbeidsgiverUtland(MottatteOpplysninger mottatteOpplysninger) {
        return !mottatteOpplysninger.getMottatteOpplysningerData().foretakUtland.isEmpty()
            && mottatteOpplysninger.getMottatteOpplysningerData().foretakUtland.stream()
            .anyMatch(foretakUtland -> Boolean.FALSE.equals(foretakUtland.getSelvstendigNæringsvirksomhet()));
    }

    private String hentBegrunnelsekodeTilForkortetPeriode(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getAvklartefakta().stream()
            .filter(avklartfakta -> avklartfakta.getType() == Avklartefaktatyper.AARSAK_ENDRING_PERIODE)
            .map(Avklartefakta::getFakta)
            .map(Endretperiode::valueOf)
            .map(Endretperiode::getKode)
            .findFirst()
            .orElse(null);
    }

    private String hentBegrunnelseFritekst(Prosessinstans prosessinstans) {
        return prosessinstans.getData(ProsessDataKey.BEGRUNNELSE_FRITEKST);
    }

    private String hentSaksbehandler(Prosessinstans prosessinstans, Behandlingsresultat behandlingsresultat) {
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandler) && behandlingsresultat.erArt16EtterUtlandMedRegistrertSvar()) {
            saksbehandler = behandlingsresultat.finnAnmodningsperiode().map(Anmodningsperiode::getAnmodetAv)
                .orElse(prosessinstans.getBehandling().getFagsak().getRegistrertAv());
        }
        return saksbehandler;
    }

    private DoksysBrevbestilling lagDoksysBrevbestilling(Produserbaredokumenter produserBartDokument, String saksbehandler, String fritekst) {
        return new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(produserBartDokument)
            .medAvsenderID(saksbehandler)
            .medFritekst(fritekst)
            .build();
    }
}
