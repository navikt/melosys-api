package no.nav.melosys.service.dokument;

import java.util.*;

import io.getunleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.BrevkopiRegel;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.mapper.BrevmottakerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.Preferanse.PreferanseEnum.RESERVERT_FRA_A1;
import static no.nav.melosys.domain.brev.BrevkopiRegel.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.featuretoggle.ToggleName.FOLKETRYGDEN_MVP;

@Service
public class BrevmottakerService {
    private static final Logger log = LoggerFactory.getLogger(BrevmottakerService.class);

    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final Unleash unleash;

    public BrevmottakerService(AvklarteVirksomheterService avklarteVirksomheterService,
                               UtenlandskMyndighetService utenlandskMyndighetService,
                               BehandlingsresultatService behandlingsresultatService,
                               LovvalgsperiodeService lovvalgsperiodeService,
                               Unleash unleash) {
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.unleash = unleash;
    }

    /**
     * Brukes av doksys for å utlede mottaker-rolle
     */
    Mottakerroller avklarMottakerRolleFraDokument(Produserbaredokumenter produserbartDokument) {
        return switch (produserbartDokument) {
            case AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK,
                INNVILGELSE_YRKESAKTIV, IKKE_YRKESAKTIV_VEDTAKSBREV -> Mottakerroller.BRUKER;
            case INNVILGELSE_ARBEIDSGIVER, AVSLAG_ARBEIDSGIVER -> Mottakerroller.ARBEIDSGIVER;
            case ANMODNING_UNNTAK, ATTEST_A1 -> Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET;
            default -> throw new TekniskException("Valg av mottakerRolle støttes ikke for " + produserbartDokument);
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

    private void leggTilKopier(long behandlingId, Mottakerliste mottakerliste, Collection<BrevkopiRegel> brevkopiRegler) {
        if (brevkopiRegler.contains(BRUKER_FÅR_KOPI)) {
            mottakerliste.getKopiMottakere().add(Mottakerroller.BRUKER);
        }
        if (brevkopiRegler.contains(ARBEIDSGIVER_FÅR_KOPI)) {
            mottakerliste.getKopiMottakere().add(Mottakerroller.ARBEIDSGIVER);
        }
        if (!unleash.isEnabled(FOLKETRYGDEN_MVP) && brevkopiRegler.contains(SKATT_FÅR_KOPI)) {
            mottakerliste.getFasteMottakere().add(NorskMyndighet.SKATTEETATEN);
        }
        if (brevkopiRegler.contains(UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI)) {
            mottakerliste.getKopiMottakere().add(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        }

        if (brevkopiRegler.contains(UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI_HVIS_IKKE_ART_8_2)) {
            Optional.ofNullable(lovvalgsperiodeService.hentLovvalgsperiode(behandlingId)).ifPresent(lovvalgsperiode -> {
                    if (lovvalgsperiode.getBestemmelse() != Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2) {
                        mottakerliste.getKopiMottakere().add(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
                    }
                }
            );
        }
    }

    public Mottaker avklarMottaker(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) {
        List<Mottaker> mottakere = avklarMottakere(produserbartDokument, mottaker, behandling, false, false);
        if (mottakere.isEmpty()) {
            throw new FunksjonellException("Finner ikke avklart mottaker for produserbart dokument " + produserbartDokument.getKode() + " og rolle " + mottaker.getRolle() + " for behandling " + behandling.getId());
        }
        if (mottakere.size() > 1) {
            throw new FunksjonellException("Flere enn én mottaker ble funnet for produserbart dokument " + produserbartDokument.getKode() + " og rolle " + mottaker.getRolle() + " for behandling " + behandling.getId());
        }
        return mottakere.get(0);
    }

    public List<Mottaker> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) {
        return avklarMottakere(produserbartDokument, mottaker, behandling, false);
    }

    public List<Mottaker> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning) {
        return avklarMottakere(produserbartDokument, mottaker, behandling, forhåndsvisning, true);
    }

    public List<Mottaker> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning, boolean kunAvklarteVirksomheter) {
        return switch (mottaker.getRolle()) {
            case BRUKER -> avklarMottakereForBruker(produserbartDokument, behandling, forhåndsvisning);
            case VIRKSOMHET -> avklarMottakereForVirksomhet(behandling);
            case ARBEIDSGIVER -> avklarMottakereForArbeidsgiver(behandling, kunAvklarteVirksomheter);
            case UTENLANDSK_TRYGDEMYNDIGHET -> avklarMottakereForUtenlandskTrygdeyndighet(mottaker, behandling, produserbartDokument);
            case NORSK_MYNDIGHET -> avklarMottakereForNorskMyndighet(mottaker);
            case FULLMEKTIG -> avklarMottakereForFullmektig(behandling.getFagsak());
            default -> throw new FunksjonellException("%s støttes ikke.".formatted(mottaker.getRolle()));
        };
    }

    private List<Mottaker> avklarMottakereForBruker(Produserbaredokumenter produserbartDokument, Behandling behandling, boolean forhåndsvisning) {
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

        List<Mottaker> mottakere = new ArrayList<>();
        Optional<Aktoer> fullmektig = fagsak.finnRepresentantEllerFullmektig(Representerer.BRUKER);
        if (fullmektig.isPresent()) {
            mottakere.add(Mottaker.av(fullmektig.get()));
            if (tilBegge) {
                mottakere.add(Mottaker.av(bruker));
            }
        } else {
            mottakere.add(Mottaker.av(bruker));
        }
        return mottakere;
    }

    private List<Mottaker> avklarMottakereForFullmektig(Fagsak fagsak) {
        Optional<Aktoer> fullmektig = fagsak.finnRepresentantEllerFullmektig(Representerer.BRUKER);
        if (fullmektig.isPresent()) {
            return List.of(Mottaker.av(fullmektig.get()));
        } else {
            throw new FunksjonellException("Finner ikke fullmektig for bruker");
        }
    }

    private List<Mottaker> avklarMottakereForVirksomhet(Behandling behandling) {
        Aktoer virksomhet = behandling.getFagsak().hentVirksomhet();
        if (virksomhet == null) {
            throw new FunksjonellException("Virksomhet er ikke registrert.");
        }
        return List.of(Mottaker.av(virksomhet));
    }

    private List<Mottaker> avklarMottakereForArbeidsgiver(Behandling behandling, boolean kunAvklarteVirksomheter) {
        Fagsak fagsak = behandling.getFagsak();
        Optional<Aktoer> fullmektig = fagsak.finnRepresentantEllerFullmektig(Representerer.ARBEIDSGIVER);
        if (fullmektig.isPresent()) {
            return Collections.singletonList(Mottaker.av(fullmektig.get()));
        } else {
            return kunAvklarteVirksomheter ? avklarArbeidsgiverFraAvklarteVirksomheter(behandling) : avklarArbeidsgiverFraAlleVirksomheter(behandling);
        }
    }

    private List<Mottaker> avklarArbeidsgiverFraAvklarteVirksomheter(Behandling behandling) {
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

    private List<Mottaker> avklarArbeidsgiverFraAlleVirksomheter(Behandling behandling) {
        Set<String> arbeidsgiverOrgnumre = new HashSet<>();
        arbeidsgiverOrgnumre.addAll(finnOrgNummerFraArbeidsforhold(behandling));
        arbeidsgiverOrgnumre.addAll(behandling.getMottatteOpplysninger().getMottatteOpplysningerData().hentAlleOrganisasjonsnumre());
        return avklarArbeidsgiver(arbeidsgiverOrgnumre);
    }

    private Set<String> finnOrgNummerFraArbeidsforhold(Behandling behandling) {
        return behandling.finnArbeidsforholdDokument().map(ArbeidsforholdDokument::hentOrgnumre).orElse(Collections.emptySet());
    }

    private List<Mottaker> avklarArbeidsgiver(Set<String> arbeidsgiverOrgnumre) {
        return arbeidsgiverOrgnumre.stream()
            .map(BrevmottakerService::lagMottakerForArbeidsgiver)
            .toList();
    }

    private static Mottaker lagMottakerForArbeidsgiver(String orgnr) {
        Mottaker arbeidsgiver = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER);
        arbeidsgiver.setOrgnr(orgnr);
        return arbeidsgiver;
    }

    private List<Mottaker> avklarMottakereForUtenlandskTrygdeyndighet(Mottaker mottaker, Behandling behandling, Produserbaredokumenter produserbartDokument) {
        if (mottaker.getOrgnr() != null) {
            // Norsk myndighet har orgnummer.
            return Collections.singletonList(mottaker);
        } else {
            // Utenlandsk myndighet
            Map<UtenlandskMyndighet, Mottaker> utenlandskMyndighetMottakerMap
                = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);

            if (produserbartDokument == UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV && utenlandskMyndighetMottakerMap.isEmpty()) {
                throw new FunksjonellException("Du kan ikke sende brev til trygdemyndigheten i landet du har valgt, fordi korrekt adresse er ukjent.");
            }

            if (produserbartDokument == ATTEST_A1 && kanReservereMotA1(behandling)) {
                return utenlandskMyndighetMottakerMap.entrySet()
                    .stream()
                    .filter(e -> myndighetØnskerA1(e.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            } else {
                return new ArrayList<>(utenlandskMyndighetMottakerMap.values());
            }
        }
    }

    private boolean kanReservereMotA1(Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode =
            behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).hentLovvalgsperiode();
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

    private List<Mottaker> avklarMottakereForNorskMyndighet(Mottaker mottaker) {
        if (mottaker.getOrgnr() != null) {
            return Collections.singletonList(mottaker);
        }
        throw new FunksjonellException("Forventer orgnr i mottaker med rolle NORSK_MYNDIGHET");
    }
}
