package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

@Service
@Primary
public class AvklarteVirksomheterService {
    private static final Logger log = LoggerFactory.getLogger(AvklarteVirksomheterService.class);

    protected final AvklartefaktaService avklartefaktaService;
    protected final OrganisasjonOppslagService organisasjonOppslagService;
    protected final BehandlingService behandlingService;
    protected final KodeverkService kodeverkService;

    public AvklarteVirksomheterService(AvklartefaktaService avklartefaktaService,
                                       OrganisasjonOppslagService organisasjonOppslagService,
                                       BehandlingService behandlingService,
                                       KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.organisasjonOppslagService = organisasjonOppslagService;
        this.behandlingService = behandlingService;
        this.kodeverkService = kodeverkService;
    }

    public List<AvklartVirksomhet> hentUtenlandskeVirksomheter(Behandling behandling) {
        MottatteOpplysninger mottatteOpplysninger = behandling.getMottatteOpplysninger();
        if (mottatteOpplysninger == null) {
            return Collections.emptyList();
        }

        MottatteOpplysningerData mottatteOpplysningerData = mottatteOpplysninger.getMottatteOpplysningerData();
        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());

        return mottatteOpplysningerData.foretakUtland.stream()
            .filter(uf -> avklarteOrgnumreOgUuider.contains(uf.uuid))
            .map(AvklartVirksomhet::new)
            .toList();
    }

    Set<String> hentNorskeSelvstendigeForetakOrgnumre(Behandling behandling) {
        MottatteOpplysninger mottatteOpplysninger = behandling.getMottatteOpplysninger();
        if (mottatteOpplysninger == null) {
            return Collections.emptySet();
        }

        MottatteOpplysningerData mottatteOpplysningerData = mottatteOpplysninger.getMottatteOpplysningerData();
        Set<String> organisasjonsnumre = mottatteOpplysningerData.selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .collect(Collectors.toSet());

        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());
        organisasjonsnumre.retainAll(avklarteOrgnumreOgUuider);
        return organisasjonsnumre;
    }

    public Set<String> hentNorskeArbeidsgivendeOrgnumre(Behandling behandling) {
        MottatteOpplysninger mottatteOpplysninger = behandling.getMottatteOpplysninger();
        if (mottatteOpplysninger == null) {
            return Collections.emptySet();
        }

        Set<String> arbeidsgivendeOrgnumre = finnOrgNummerFraArbeidsforhold(behandling);
        MottatteOpplysningerData mottatteOpplysningerData = mottatteOpplysninger.getMottatteOpplysningerData();
        arbeidsgivendeOrgnumre.addAll(mottatteOpplysningerData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere);

        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());
        arbeidsgivendeOrgnumre.retainAll(avklarteOrgnumreOgUuider);
        return arbeidsgivendeOrgnumre;
    }

    private Set<String> finnOrgNummerFraArbeidsforhold(Behandling behandling) {
        return behandling.finnArbeidsforholdDokument().map(ArbeidsforholdDokument::hentOrgnumre).orElse(new HashSet<>());
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendigeForetak(Behandling behandling) {
        return hentNorskeSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendigeForetak(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        Set<String> selvstendigeForetakOrgnumre = hentNorskeSelvstendigeForetakOrgnumre(behandling);
        return organisasjonOppslagService.hentOrganisasjoner(selvstendigeForetakOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.SELVSTENDIG))
            .toList();
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere(Behandling behandling) {
        return hentNorskeArbeidsgivere(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        Set<String> arbeidsgivendeOrgnumre = hentNorskeArbeidsgivendeOrgnumre(behandling);
        return organisasjonOppslagService.hentOrganisasjoner(arbeidsgivendeOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.LOENNET_ARBEID, org.organisasjonDetaljer.opphoersdato))
            .toList();
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling) {
        return hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        List<AvklartVirksomhet> norskeVirksomheter = new ArrayList<>(hentNorskeArbeidsgivere(behandling, adressekonverterer));
        Set<String> norskeVirksomheterOrgnr = norskeVirksomheter.stream()
            .map(AvklartVirksomhet::getOrgnr).collect(Collectors.toSet());

        List<AvklartVirksomhet> norskeSelvstendigeForetak = hentNorskeSelvstendigeForetak(behandling, adressekonverterer);
        for (AvklartVirksomhet norskSelvstendigForetak : norskeSelvstendigeForetak) {
            if (norskeVirksomheterOrgnr.contains(norskSelvstendigForetak.getOrgnr())) {
                log.warn("Fant selvstendige foretak med samme orgnummer({}) som allerede er hentet fra norskeArbeidsgivere",
                    norskSelvstendigForetak.getOrgnr());
            } else {
                norskeVirksomheter.add(norskSelvstendigForetak);
            }
        }

        return norskeVirksomheter;
    }

    public int hentAntallAvklarteVirksomheter(Behandling behandling) {
        return hentNorskeArbeidsgivendeOrgnumre(behandling).size()
            + hentNorskeSelvstendigeForetakOrgnumre(behandling).size()
            + hentUtenlandskeVirksomheter(behandling).size();
    }

    public boolean harOpphørtAvklartVirksomhet(Behandling behandling) {
        return hentAlleNorskeVirksomheter(behandling).stream().anyMatch(AvklartVirksomhet::erOpphoert);
    }

    @Transactional
    public void lagreVirksomheterSomAvklartefakta(Long behandlingID, List<String> virksomhetIDer) {
        validerVirksomhetIDerGyldige(behandlingID, virksomhetIDer);

        avklartefaktaService.slettAvklarteFakta(behandlingID, VIRKSOMHET);

        for (String virksomhetID : virksomhetIDer) {
            lagreVirksomhetSomAvklartfakta(virksomhetID, behandlingID);
        }
    }

    public void lagreVirksomhetSomAvklartfakta(String virksomhetID, Long behandlingID) {
        avklartefaktaService.leggTilAvklarteFakta(behandlingID, VIRKSOMHET, VIRKSOMHET.getKode(), virksomhetID, Avklartefakta.VALGT_FAKTA);
    }

    private void validerVirksomhetIDerGyldige(Long behandlingID, List<String> virksomhetIDer) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        for (String virksomhetID : virksomhetIDer) {
            if (!erVirksomhetIDGyldig(virksomhetID, behandling)) {
                throw new FunksjonellException("VirksomhetID " + virksomhetID + " hører ikke til noen av arbeidsforholdene");
            }
        }
    }

    private boolean erVirksomhetIDGyldig(String virksomhetID, Behandling behandling) {
        MottatteOpplysninger mottatteOpplysninger = behandling.getMottatteOpplysninger();
        if (mottatteOpplysninger == null) {
            return false;
        }

        MottatteOpplysningerData mottatteOpplysningerData = mottatteOpplysninger.getMottatteOpplysningerData();
        return erVirksomhetForetakUtland(virksomhetID, mottatteOpplysningerData)
            || erVirksomhetSelvstendigForetakEllerLagtInnManuelt(virksomhetID, mottatteOpplysningerData)
            || erVirksomhetArbeidNorge(virksomhetID, behandling);
    }

    private boolean erVirksomhetForetakUtland(String uuid, MottatteOpplysningerData mottatteOpplysningerData) {
        return mottatteOpplysningerData.hentUtenlandskeArbeidsgivereUuid().contains(uuid);
    }

    private boolean erVirksomhetSelvstendigForetakEllerLagtInnManuelt(String orgnr, MottatteOpplysningerData mottatteOpplysningerData) {
        return mottatteOpplysningerData.hentAlleOrganisasjonsnumre().contains(orgnr);
    }

    private boolean erVirksomhetArbeidNorge(String orgnr, Behandling behandling) {
        return behandling.finnArbeidsforholdDokument()
            .orElseThrow(() -> new TekniskException("Finner ikke arbeidsforholddokument")).hentOrgnumre().contains(orgnr);
    }

    StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.organisasjonDetaljer.hentStrukturertForretningsadresse();
        if (adresse == null || StringUtils.isEmpty(adresse.getPostnummer())) {
            adresse = org.organisasjonDetaljer.hentStrukturertPostadresse();
        }
        if (StringUtils.isEmpty(adresse.getGatenavn())) {
            adresse.setGatenavn(" ");
        }
        if (adresse.erNorsk()) {
            adresse.setPoststed(
                kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnummer()));
        } else if (StringUtils.isEmpty(adresse.getPostnummer())) {
            //Utenlandske adresser har ikke alltid postnummer
            adresse.setPostnummer(" ");
        }
        return adresse;
    }
}
