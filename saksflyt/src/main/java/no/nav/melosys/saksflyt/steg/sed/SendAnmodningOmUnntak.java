package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import tools.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.unntak.AnmodningUnntakKonstanter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.YTTERLIGERE_INFO_SED;

@Component
public class SendAnmodningOmUnntak extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(SendAnmodningOmUnntak.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;
    private final AnmodningsperiodeService anmodningsperiodeService;


    public SendAnmodningOmUnntak(EessiService eessiService,
                                 BrevBestiller brevBestiller,
                                 BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 AnmodningsperiodeService anmodningsperiodeService) {
        super(eessiService, behandlingsresultatService);
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_ANMODNING_OM_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Behandling behandling = prosessinstans.getBehandling();
        LocalDateTime svarFristDato = LocalDateTime.now().plusMonths(AnmodningUnntakKonstanter.SVARFRIST_MÅNEDER);
        behandling.setDokumentasjonSvarfristDato(svarFristDato.atZone(AnmodningUnntakKonstanter.TIME_ZONE_ID).toInstant());
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandlingService.lagre(behandling);
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(behandling.getId());

        sendUtland(BucType.LA_BUC_01, prosessinstans, hentDokumentReferanser(prosessinstans));
    }

    private Collection<DokumentReferanse> hentDokumentReferanser(Prosessinstans prosessinstans) {
        return prosessinstans.getData(ProsessDataKey.VEDLEGG_SED,
            new TypeReference<>() {
            }, Collections.emptySet());
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) {
        brevBestiller.bestill(lagBrevBestilling(prosessinstans));
    }

    private DoksysBrevbestilling lagBrevBestilling(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(prosessinstans.getBehandling().getId());
        return new DoksysBrevbestilling.Builder().medProduserbartDokument(Produserbaredokumenter.ANMODNING_UNNTAK)
            .medAvsenderID(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET))
            .medBehandling(behandling)
            .medYtterligereInformasjon(prosessinstans.getData(YTTERLIGERE_INFO_SED))
            .build();
    }

    @Override
    protected Boolean hentErFjernarbeidTWFA(Behandlingsresultat behandlingsresultat, Prosessinstans prosessinstans) {
        Boolean fraAnmodningsperiode = behandlingsresultat.hentAnmodningsperiode().getErFjernarbeidTWFA();
        if (fraAnmodningsperiode != null) {
            return fraAnmodningsperiode;
        }

        // Kolonnen er null selv om saksbehandler svarte. Det skjer når periodene er redigert mellom anmodning og
        // sending: lagreAnmodningsperioder sletter og gjenoppretter radene, og AnmodningsperiodeSkrivDto.til()
        // bygger dem kun fra DTO-feltene — er_fjernarbeid_twfa følger ikke med. Ingenting sperrer for det i dette
        // vinduet: både sendt_utland og Behandlingsstatus.ANMODNING_UNNTAK_SENDT settes lenger opp i utfør(), altså
        // etter at vinduet er over. Feiler et tidligere steg, er vinduet timer eller dager til noen restarter.
        //
        // Prosessdataen overlever redigeringen, så den brukes som kilde. Uten dette ville A001 blitt sendt til
        // utlandet uten rammeavtale-flagget.
        Boolean fraProsessdata = prosessinstans.getData(ProsessDataKey.ER_FJERNARBEID_TWFA, Boolean.class);
        if (fraProsessdata != null) {
            // Fyll kolonnen tilbake. Fallbacken alene redder bare SED-en; kolonnen ville blitt stående null og saken
            // forsvunnet ut av rapporteringstallene — som er hele grunnen til at kolonnen finnes. V170 kjører ikke
            // på nytt.
            log.info("Behandling {}: er_fjernarbeid_twfa var ikke satt på anmodningsperioden, leser {} fra "
                + "prosessdataen og reparerer raden", behandlingsresultat.getId(), fraProsessdata);
            anmodningsperiodeService.reparerManglendeErFjernarbeidTWFA(behandlingsresultat.getId(), fraProsessdata);
        }
        return fraProsessdata;
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        Anmodningsperiode anmodningsperiode = behandlingsresultat.hentAnmodningsperiode();
        return behandlingsresultat.erAnmodningOmUnntak()
            && (anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_2);
    }
}
