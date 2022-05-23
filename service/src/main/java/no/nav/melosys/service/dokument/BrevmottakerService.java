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
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.mapper.BrevmottakerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.Preferanse.PreferanseEnum.RESERVERT_FRA_A1;
import static no.nav.melosys.domain.brev.BrevkopiRegel.*;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Service
public class BrevmottakerService {
    private static final Logger log = LoggerFactory.getLogger(BrevmottakerService.class);

    private final KontaktopplysningService kontaktopplysningService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    public BrevmottakerService(KontaktopplysningService kontaktopplysningService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               UtenlandskMyndighetService utenlandskMyndighetService,
                               BehandlingsresultatService behandlingsresultatService,
                               TrygdeavgiftsberegningService trygdeavgiftsberegningService,
                               LovvalgsperiodeService lovvalgsperiodeService, BehandlingService behandlingService) {
        this.kontaktopplysningService = kontaktopplysningService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.trygdeavgiftsberegningService = trygdeavgiftsberegningService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.behandlingService = behandlingService;
    }

    Aktoersroller avklarMottakerRolleFraDokument(Produserbaredokumenter produserbartDokument) {
        return switch (produserbartDokument) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV -> BRUKER;
            case INNVILGELSE_ARBEIDSGIVER, AVSLAG_ARBEIDSGIVER -> ARBEIDSGIVER;
            case ANMODNING_UNNTAK, ATTEST_A1 -> TRYGDEMYNDIGHET;
            default -> throw new TekniskException("Valg av mottakerRolle støttes ikke for " + produserbartDokument);
        };
    }

    public Aktoer avklarMottaker(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) {
        List<Aktoer> mottakere = avklarMottakere(produserbartDokument, mottaker, behandling, false, false);
        if (mottakere.size() < 1) {
            throw new FunksjonellException("Finner ikke avklart mottaker for produserbart dokument " + produserbartDokument.getKode() + " og rolle " + mottaker.getRolle() + " for behandling " + behandling.getId());
        }
        if (mottakere.size() > 1) {
            throw new FunksjonellException("Flere enn én mottaker ble funnet for produserbart dokument " + produserbartDokument.getKode() + " og rolle " + mottaker.getRolle() + " for behandling " + behandling.getId());
        }
        return mottakere.get(0);
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) {
        return avklarMottakere(produserbartDokument, mottaker, behandling, false);
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning) {
        return avklarMottakere(produserbartDokument, mottaker, behandling, forhåndsvisning, true);
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning, boolean kunAvklarteVirksomheter) {
        return switch (mottaker.getRolle()) {
            case BRUKER -> avklarMottakereForBruker(produserbartDokument, behandling, forhåndsvisning);
            case ARBEIDSGIVER -> avklarMottakereForArbeidsgiver(behandling, kunAvklarteVirksomheter);
            case TRYGDEMYNDIGHET -> avklarMottakereForMyndigheter(mottaker, behandling, produserbartDokument);
            default -> throw new FunksjonellException("%s støttes ikke.".formatted(mottaker.getRolle()));
        };
    }

    @Transactional
    public Mottakerliste hentMottakerliste(Produserbaredokumenter produserbartdokument, long behandlingId) {
        Mottakerliste mottakerliste = ofNullable(BrevmottakerMapper.BREV_MOTTAKER_MAP.get(produserbartdokument))
            .orElseThrow(() -> new IkkeFunnetException("Mangler mapping av mottakere for " + produserbartdokument));

        Mottakerliste mottakerListeKopi = new Mottakerliste.Builder()
            .medHovedMottaker(mottakerliste.getHovedMottaker())
            .build();

        if (mottakerliste.kanHaKopier()) {
            leggTilKopier(behandlingId, mottakerListeKopi, mottakerliste.getBrevkopiRegler());
        }

        return mottakerListeKopi;
    }

    private List<Aktoer> avklarMottakereForBruker(Produserbaredokumenter produserbartDokument, Behandling behandling, boolean forhåndsvisning) {
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
        Optional<Aktoer> representant = fagsak.finnRepresentant(Representerer.BRUKER);
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
    private List<Aktoer> avklarMottakereForArbeidsgiver(Behandling behandling, boolean kunAvklarteVirksomheter) {
        Fagsak fagsak = behandling.getFagsak();
        Optional<Aktoer> representant = fagsak.finnRepresentant(Representerer.ARBEIDSGIVER);
        if (representant.isPresent()) {
            return Collections.singletonList(representant.get());
        } else {
            return kunAvklarteVirksomheter ? avklarArbeidsgiverFraAvklarteVirksomheter(behandling) : avklarArbeidsgiverFraAlleVirksomheter(behandling);
        }
    }

    private List<Aktoer> avklarArbeidsgiverFraAvklarteVirksomheter(Behandling behandling) {
        Set<String> arbeidsgivendeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        if (arbeidsgivendeOrgnumre.isEmpty()) {
            if (avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).isEmpty()) {
                throw new FunksjonellException("Arbeidsgiver er ikke registrert.");
            } else {
                log.debug("Melosys sender ikke brev til utenlandske arbeidsgivere uten orgnr.");
                return Collections.emptyList();
            }
        }
        return avklarArbeidsgiver(arbeidsgivendeOrgnumre);
    }

    private List<Aktoer> avklarArbeidsgiverFraAlleVirksomheter(Behandling behandling) {
        Set<String> arbeidsgiverOrgnumre = new HashSet<>();
        arbeidsgiverOrgnumre.addAll(behandling.hentArbeidsforholdDokument().hentOrgnumre());
        arbeidsgiverOrgnumre.addAll(behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentAlleOrganisasjonsnumre());
        return avklarArbeidsgiver(arbeidsgiverOrgnumre);
    }

    private List<Aktoer> avklarArbeidsgiver(Set<String> arbeidsgiverOrgnumre) {
        return arbeidsgiverOrgnumre.stream()
            .map(BrevmottakerService::lagAktoerForArbeidsgiver)
            .collect(Collectors.toList());
    }

    private static Aktoer lagAktoerForArbeidsgiver(String orgnr) {
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr(orgnr);
        return arbeidsgiver;
    }

    private List<Aktoer> avklarMottakereForMyndigheter(Mottaker mottaker,
                                                       Behandling behandling,
                                                       Produserbaredokumenter produserbartDokument) {
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

    private boolean kanReservereMotA1(Behandling behandling) {
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

    public Kontaktopplysning hentKontaktopplysning(String saksnummer, Aktoer mottaker) {
        if (mottaker != null && List.of(ARBEIDSGIVER, REPRESENTANT).contains(mottaker.getRolle())) {
            return kontaktopplysningService.hentKontaktopplysning(saksnummer, mottaker.getOrgnr()).orElse(null);
        }
        return null;
    }

    private void leggTilKopier(long behandlingId, Mottakerliste mottakerliste, Collection<BrevkopiRegel> brevkopiRegler) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        boolean brukerHarFullmektig = behandling.getFagsak().finnRepresentant(Representerer.BRUKER).isPresent();

        if (brevkopiRegler.contains(BRUKER_FÅR_KOPI) ||
            (brevkopiRegler.contains(BRUKER_FÅR_KOPI_HVIS_FULLMEKTIG_FINNES) && brukerHarFullmektig)) {
            mottakerliste.getKopiMottakere().add(BRUKER);
        }
        if (brevkopiRegler.contains(ARBEIDSGIVER_FÅR_KOPI)) {
            mottakerliste.getKopiMottakere().add(ARBEIDSGIVER);
        }
        if (brevkopiRegler.contains(SKATT_FÅR_KOPI)) {
            mottakerliste.getFasteMottakere().add(SKATT);
        }

        if (brevkopiRegler.contains(UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI_HVIS_IKKE_ART_8_2)) {
            Optional.ofNullable(lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId())).ifPresent(lovvalgsperiode -> {
                    if (lovvalgsperiode.getBestemmelse() != Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2) {
                        mottakerliste.getKopiMottakere().add(TRYGDEMYNDIGHET);
                    }
                }
            );
        }

        Optional<Trygdeavgiftsberegningsresultat> trygdeavgiftsberegningsresultat = trygdeavgiftsberegningService.finnBeregningsresultat(behandling.getId());

        trygdeavgiftsberegningsresultat.ifPresent(resultat -> {
            if (brevkopiRegler.contains(ARBEIDSGIVER_FÅR_KOPI_HVIS_IKKE_SELVBETALENDE_BRUKER) && resultat.erIkkeSelvbetalendeBruker()) {
                mottakerliste.getKopiMottakere().add(ARBEIDSGIVER);
            }

            if (brevkopiRegler.contains(SKATT_FÅR_KOPI_HVIS_AVGIFTSPLIKTIG_INNTEKT) && resultat.harAvgiftspliktigInntekt()) {
                mottakerliste.getFasteMottakere().add(SKATT);
            }
        });
    }
}
