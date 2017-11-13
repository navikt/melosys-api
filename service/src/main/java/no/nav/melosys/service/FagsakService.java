package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.medl.Medl2Fasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FagsakService {

    private FagsakRepository fagsakRepository;

    private TpsFasade tpsFasade;

    private AaregFasade aaregFasade;

    private EregFasade eregFasade;

    private Medl2Fasade medl2Fasade;

    private InntektFasade inntektFasade;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, TpsFasade tpsFasade, AaregFasade aaregFasade, EregFasade eregFasade, Medl2Fasade medl2Fasade, InntektFasade inntektFasade) {
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medl2Fasade = medl2Fasade;
        this.inntektFasade = inntektFasade;
    }

    public List<Fagsak> hentFagsaker(RolleType rolleType, String aktørID) {
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    public Fagsak hentFagsak(Long saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public Fagsak nyFagsak(String fnr) {

        try {
            Optional<Saksopplysning> personSaksopplysning = Optional.ofNullable(hentPerson(fnr));
            Optional<Saksopplysning> medlemskapSaksopplysning = Optional.ofNullable(hentMedlemskap(fnr));
            Optional<Saksopplysning> arbeidsforholdSaksopplysning = Optional.ofNullable(hentArbeidsforhold(fnr));
            Optional<Saksopplysning> inntektSaksopplysning = Optional.ofNullable(hentInntekt(fnr));

            Set<Saksopplysning> saksopplysninger = new HashSet<>();

            personSaksopplysning.ifPresent(saksopplysninger::add);
            medlemskapSaksopplysning.ifPresent(saksopplysninger::add);
            arbeidsforholdSaksopplysning.ifPresent(saksopplysninger::add);
            inntektSaksopplysning.ifPresent(saksopplysninger::add);

            Set<String> orgnumre = new HashSet<>();

            arbeidsforholdSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraArbeidsforhold(saksopplysning)));
            inntektSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraInntekt(saksopplysning)));

            if (!orgnumre.isEmpty())
                saksopplysninger.addAll(hentOrganisasjoner(orgnumre));

            Fagsak fagsak = new Fagsak().withBehandlinger(Collections.singletonList(new Behandling().withSaksopplysninger(saksopplysninger)));
            fagsakRepository.save(fagsak);

            return fagsak;

        } catch (SikkerhetsbegrensningException e) {
            return null;
        }
    }

    private Saksopplysning hentPerson(String fnr) throws SikkerhetsbegrensningException {
        // TODO: Informasjonsbehov.FAMILIERELASJONER kommer i runde 2
        final List<Informasjonsbehov> informasjonsbehov = Collections.singletonList(Informasjonsbehov.ADRESSE);
        try {
            return tpsFasade.hentPerson(fnr, informasjonsbehov);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

    private Saksopplysning hentMedlemskap(String fnr) throws SikkerhetsbegrensningException {
        try {
            return medl2Fasade.getPeriodeListe(fnr);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

    private Saksopplysning hentArbeidsforhold(String fnr) throws SikkerhetsbegrensningException {
        try {
            return aaregFasade.finnArbeidsforholdPrArbeidstaker(fnr, AaregFasade.REGELVERK_A_ORDNINGEN);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

    private Saksopplysning hentInntekt(String fnr) throws SikkerhetsbegrensningException {
        final YearMonth tom = YearMonth.now();
        final YearMonth fom = tom.minusMonths(12);
        try {
            return inntektFasade.hentInntektListe(fnr, fom, tom);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

    private List<Saksopplysning> hentOrganisasjoner(Set<String> orgnumre) throws SikkerhetsbegrensningException {
        List<Saksopplysning> saksopplysninger = new ArrayList<>();

        for (String orgnr : orgnumre) {
            Saksopplysning saksopplysning = hentOrganisasjon(orgnr);
            if (saksopplysning != null) {
                saksopplysninger.add(saksopplysning);
            }
        }
        return saksopplysninger;
    }

    private static Set<String> hentOrgnumreFraArbeidsforhold(Saksopplysning saksopplysning) {
        return ((ArbeidsforholdDokument) saksopplysning.getDokument()).getArbeidsforhold().stream()
                .flatMap(arbeidsforhold -> Stream.of(arbeidsforhold.getArbeidsgiverID(), arbeidsforhold.getOpplysningspliktigID()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Set<String> hentOrgnumreFraInntekt(Saksopplysning saksopplysning) {
        return ((InntektDokument) saksopplysning.getDokument()).getArbeidsInntektMaanedListe().stream()
                .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
                .filter(Objects::nonNull)
                .map(ArbeidsInntektInformasjon::getInntektListe)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Inntekt::getVirksomhetID)
                .collect(Collectors.toSet());
    }

    private Saksopplysning hentOrganisasjon(String orgnr) throws SikkerhetsbegrensningException {
        try {
            return eregFasade.hentOrganisasjon(orgnr);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

}
