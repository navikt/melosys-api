package no.nav.melosys.saksflyt.agent.iv;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_3A;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART11_4_1;

/**
 * Avklarer hvilken utenlandsk myndighet er part i saken.
 *
 * Transisjoner:
 *  IV_AVKLAR_MYNDIGHET -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvklarMyndighet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(no.nav.melosys.saksflyt.agent.iv.AvklarMyndighet.class);

    private final AvklartefaktaService avklarteFaktaService;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final FagsakService fagsakService;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public AvklarMyndighet(AvklartefaktaService avklarteFaktaService,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           FagsakService fagsakService,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.avklarteFaktaService = avklarteFaktaService;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.fagsakService = fagsakService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        log.info("AvklarMyndighet initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_AVKLAR_MYNDIGHET;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findWithSaksbehandlingById(behandlingID)
            .orElseThrow(() -> new TekniskException("Behandlingsresultat " + behandlingID + " finnes ikke."));

        if (SendBrevValidator.innvilgelsesbrevSkalSendes(behandlingsresultat.getType(), behandlingsresultat.getLovvalgsperioder().iterator().next())) {
            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            String saksnummer = fagsak.getSaksnummer();
            Aktoer myndighetPart = fagsak.hentAktørMedRolleType(Aktoersroller.MYNDIGHET);
            if (myndighetPart == null) {
                Landkoder landkode = avklarLand(behandling, behandlingsresultat);
                UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
                String institusjonsID = landkode.getKode() + ":" + myndighet.institusjonskode;
                fagsakService.leggTilAktør(saksnummer, Aktoersroller.MYNDIGHET, institusjonsID);
                log.info("Avklart landkode {} og myndighet {} for sak {}.", landkode, myndighet.institusjonskode, saksnummer);
            } else {
                log.debug("Sak {} har allerede en myndighet med kode {}", saksnummer, myndighetPart.getInstitusjonId());
            }
        }

        prosessinstans.setSteg(IV_OPPDATER_MEDL);
    }

    Landkoder avklarLand(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException, TekniskException {
        long behandlingsresultatID = behandlingsresultat.getId();
        List<Vilkaar> oppfylteVilkår = behandlingsresultat.getVilkaarsresultater().stream()
            .filter(Vilkaarsresultat::isOppfylt).map(Vilkaarsresultat::getVilkaar).collect(Collectors.toList());

        if (oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
            if (oppfylteVilkår.contains(FO_883_2004_ART11_3A)) {
                // Bruker BOSTEDSLAND.
                return Landkoder.valueOf(avklarteFaktaService.hentAvklarteFakta(behandlingsresultatID, Avklartefaktatype.BOSTEDSLAND).getFakta());
            } else {
                // Bruker FLAGGLAND
                return Landkoder.valueOf(avklarteFaktaService.hentAvklarteFakta(behandlingsresultatID, Avklartefaktatype.FLAGGLAND).getFakta());
            }
        }

        SoeknadDokument søknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
        if (oppfylteVilkår.contains(FO_883_2004_ART11_3A) && !oppfylteVilkår.contains(FO_883_2004_ART11_4_1)) {
            // Bruker land fra adressen oppgitt i søknaden.
            // N.B. bør ikke komme fra bostedsvurdering.
            return Landkoder.valueOf(søknadDokument.bosted.oppgittAdresse.landKode);
        }

        // Bruker land oppgitt i søknaden
        return Landkoder.valueOf(søknadDokument.oppholdUtland.oppholdslandKoder.get(0)); //FIXME må erstattes med søknadDokument.land (søknadsland).
    }
}
