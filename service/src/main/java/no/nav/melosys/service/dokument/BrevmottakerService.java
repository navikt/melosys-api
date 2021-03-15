package no.nav.melosys.service.dokument;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.domain.brev.BrevkopiRegel;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.Fagsak.erSakstypeFtrl;
import static no.nav.melosys.domain.Preferanse.PreferanseEnum.RESERVERT_FRA_A1;
import static no.nav.melosys.domain.brev.BrevkopiRegel.*;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Service
public class BrevmottakerService {
    private static final Logger log = LoggerFactory.getLogger(BrevmottakerService.class);
    private static final Set<Produserbaredokumenter> DOKUMENTER_TIL_BRUKER = Collections.unmodifiableSet(EnumSet.of(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
        AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV));

    private final KontaktopplysningService kontaktopplysningService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final TrygdeavgiftsberegningService trygdeavgiftsberegningService;

    @Autowired
    public BrevmottakerService(KontaktopplysningService kontaktopplysningService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               UtenlandskMyndighetService utenlandskMyndighetService,
                               BehandlingsresultatService behandlingsresultatService, TrygdeavgiftsberegningService trygdeavgiftsberegningService) {
        this.kontaktopplysningService = kontaktopplysningService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.trygdeavgiftsberegningService = trygdeavgiftsberegningService;
    }

    Aktoersroller avklarMottakerRolleFraDokument(Produserbaredokumenter produserbartDokument) throws TekniskException {
        Aktoersroller mottakerRolle;
        if (DOKUMENTER_TIL_BRUKER.contains(produserbartDokument)) {
            mottakerRolle = BRUKER;
        } else if (produserbartDokument == INNVILGELSE_ARBEIDSGIVER || produserbartDokument == AVSLAG_ARBEIDSGIVER) {
            mottakerRolle = ARBEIDSGIVER;
        } else if (produserbartDokument == ANMODNING_UNNTAK || produserbartDokument == ATTEST_A1) {
            mottakerRolle = MYNDIGHET;
        } else {
            throw new TekniskException("Valg av mottakerRolle støttes ikke for " + produserbartDokument);
        }
        return mottakerRolle;
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) throws FunksjonellException, TekniskException {
        return avklarMottakere(produserbartDokument, mottaker, behandling, false);
    }

    List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning)
        throws FunksjonellException, TekniskException {
        List<Aktoer> mottakere;
        Aktoersroller mottakerRolle = mottaker.getRolle();
        if (mottakerRolle == BRUKER) {
            mottakere = avklarMottakereForBruker(produserbartDokument, behandling, forhåndsvisning);
        } else if (mottakerRolle == ARBEIDSGIVER) {
            mottakere = avklarMottakereForArbeidsgiver(behandling);
        } else if (mottakerRolle == MYNDIGHET) {
            mottakere = avklarMottakereForMyndigheter(mottaker, behandling, produserbartDokument);
        } else {
            throw new FunksjonellException(mottakerRolle + " støttes ikke.");
        }
        return mottakere;
    }

    public Mottakerliste hentMottakerliste(Produserbaredokumenter produserbartdokument, Behandling behandling)
        throws FunksjonellException {

        Mottakerliste mottakerliste = ofNullable(BrevmottakerMapper.BREV_MOTTAKER_MAP.get(produserbartdokument))
            .orElseThrow(() -> new IkkeFunnetException("Mangler mapping av mottakere for " + produserbartdokument));

        Mottakerliste mottakerListeKopi = new Mottakerliste.Builder()
            .medHovedMottaker(mottakerliste.getHovedMottaker())
            .build();

        if (mottakerliste.kanHaKopier()) {
            leggTilKopier(behandling, mottakerListeKopi, mottakerliste.getBrevkopiRegler());
        }

        return mottakerListeKopi;
    }

    private List<Aktoer> avklarMottakereForBruker(Produserbaredokumenter produserbartDokument, Behandling behandling, boolean forhåndsvisning)
        throws FunksjonellException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        if (bruker == null) {
            throw new FunksjonellException("Bruker er ikke registrert.");
        }

        // Dokumenter til bruker sendes i utgangspunkt bare til fullmektig dersom fullmektig finnes.
        // Vedtaksbrevene er imidlertid sendt til både bruker og fullmektig (gjelder ikke forhåndsvisning).
        boolean tilBegge = false;
        if (produserbartDokument == INNVILGELSE_YRKESAKTIV ||
            produserbartDokument == INNVILGELSE_YRKESAKTIV_FLERE_LAND ||
            produserbartDokument == AVSLAG_YRKESAKTIV) {
            tilBegge = !forhåndsvisning;
        }

        List<Aktoer> mottakere = new ArrayList<>();
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        if (representant.isPresent()) {
            mottakere.add(representant.get());
            if (tilBegge) {
                mottakere.add(bruker);
            }
        } else {
            mottakere.add(bruker);
        }
        return mottakere;
    }

    // Dokumenter til arbeidsgiver sendes bare til representant når representant finnes.
    private List<Aktoer> avklarMottakereForArbeidsgiver(Behandling behandling) throws FunksjonellException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.ARBEIDSGIVER);
        if (representant.isPresent()) {
            return Collections.singletonList(representant.get());
        } else {
            return avklarArbeidsgiver(behandling);
        }
    }

    private List<Aktoer> avklarArbeidsgiver(Behandling behandling) throws FunksjonellException, TekniskException {
        Set<String> arbeidsgivendeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        if (arbeidsgivendeOrgnumre.isEmpty()) {
            if (avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).isEmpty()) {
                throw new FunksjonellException("Arbeidsgiver er ikke registrert.");
            } else {
                log.debug("Melosys sender ikke brev til utenlandske arbeidsgivere uten orgnr.");
                return Collections.emptyList();
            }
        } else {
            return arbeidsgivendeOrgnumre.stream()
                .map(BrevmottakerService::lagAktoerForArbeidsgiver)
                .collect(Collectors.toList());
        }
    }

    private static Aktoer lagAktoerForArbeidsgiver(String orgnr) {
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr(orgnr);
        return arbeidsgiver;
    }

    private List<Aktoer> avklarMottakereForMyndigheter(Mottaker mottaker,
                                                       Behandling behandling,
                                                       Produserbaredokumenter produserbartDokument) throws IkkeFunnetException {
        if (mottaker.getAktør().getOrgnr() != null) {
            // Norsk myndighet har orgnummer.
            return Collections.singletonList(mottaker.getAktør());
        } else {
            // Utenlandsk myndighet
            Map<UtenlandskMyndighet, Aktoer> utenlandskMyndighetAktoerMap
                = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);

            if (produserbartDokument == ATTEST_A1 && kanReservereMotA1(behandling)) {
                return utenlandskMyndighetAktoerMap.entrySet()
                    .stream()
                    .filter(e -> myndighetØnskerA1(e.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            } else {
                return new ArrayList<>(utenlandskMyndighetAktoerMap.values());
            }
        }
    }

    private boolean kanReservereMotA1(Behandling behandling) throws IkkeFunnetException {
        Lovvalgsperiode lovvalgsperiode =
            behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).hentValidertLovvalgsperiode();
        return lovvalgsperiode.erArtikkel12() || lovvalgsperiode.erArtikkel11_4()
            || lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B;
    }

    private boolean myndighetØnskerA1(UtenlandskMyndighet utenlandskMyndighet) {
        return utenlandskMyndighet
                .preferanser
                .stream()
                .map(Preferanse::getPreferanse)
                .noneMatch(RESERVERT_FRA_A1::equals);
    }

    public Kontaktopplysning hentKontaktopplysning(String saksnumner, Aktoer mottaker) {
        if (mottaker == null) {
            return null;
        }

        Aktoersroller mottakerRolle = mottaker.getRolle();
        if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            return kontaktopplysningService.hentKontaktopplysning(saksnumner, mottaker.getOrgnr()).orElse(null);
        } else {
            return null;
        }
    }

    private void leggTilKopier(Behandling behandling, Mottakerliste mottakerliste, Collection<BrevkopiRegel> brevkopiRegler) {
        boolean brukerHarFullmektig = behandling.getFagsak().hentRepresentant(Representerer.BRUKER).isPresent();

        if (brevkopiRegler.contains(BRUKER_FÅR_KOPI) ||
            (brevkopiRegler.contains(BRUKER_FÅR_KOPI_HVIS_FULLMEKTIG_FINNES) && brukerHarFullmektig)) {
            mottakerliste.getKopiMottakere().add(BRUKER);
        }

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = hentTrygdeavgiftresultat(behandling);

        if (erSakstypeFtrl(behandling.getFagsak().getType()) && trygdeavgiftsberegningsresultat != null) {
            if (brevkopiRegler.contains(ARBEIDSGIVER_FÅR_KOPI_HVIS_IKKE_SELVBETALENDE_BRUKER) && trygdeavgiftsberegningsresultat.ikkeSelvbetalendeBruker()) {
                mottakerliste.getKopiMottakere().add(ARBEIDSGIVER);
            }

            if (brevkopiRegler.contains(SKATT_FÅR_KOPI_HVIS_AVGIFTSPLIKTIG_INNTEKT) && trygdeavgiftsberegningsresultat.harAvgiftspliktigInntekt()) {
                mottakerliste.getFasteMottakere().add(SKATT);
            }
        }
    }

    private Trygdeavgiftsberegningsresultat hentTrygdeavgiftresultat(Behandling behandling) {
        try {
            return trygdeavgiftsberegningService.hentBeregningsresultat(behandling.getId());
        } catch (IkkeFunnetException e) {
            // I dette tilfellet er det ikke vesentlig om noe resultat ikke blir funnet
            return null;
        }
    }
}