package no.nav.melosys.service.avklartefakta;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.adresse.Adresse;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

@Service
@Primary
public class AvklarteVirksomheterService {
    protected final AvklartefaktaService avklartefaktaService;
    protected final RegisterOppslagService registerOppslagService;
    protected final BehandlingService behandlingService;
    protected final KodeverkService kodeverkService;

    @Autowired
    public AvklarteVirksomheterService(AvklartefaktaService avklartefaktaService,
                                       RegisterOppslagService registerOppslagService,
                                       BehandlingService behandlingService, KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.behandlingService = behandlingService;
        this.kodeverkService = kodeverkService;
    }

    public List<AvklartVirksomhet> hentUtenlandskeVirksomheter(Behandling behandling) {
        BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());

        return grunnlagData.foretakUtland.stream()
            .filter(uf -> avklarteOrgnumreOgUuider.contains(uf.uuid))
            .map(AvklartVirksomhet::new)
            .collect(Collectors.toList());
    }

    Set<String> hentNorskeSelvstendigeForetakOrgnumre(Behandling behandling) {
        BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        Set<String> organisasjonsnumre = grunnlagData.selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .collect(Collectors.toSet());

        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());
        organisasjonsnumre.retainAll(avklarteOrgnumreOgUuider);
        return organisasjonsnumre;
    }

    public Set<String> hentNorskeArbeidsgivendeOrgnumre(Behandling behandling) {
        ArbeidsforholdDokument arbDok = behandling.hentArbeidsforholdDokument();
        Set<String> arbeidsgivendeOrgnumre = arbDok.hentOrgnumre();
        BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        arbeidsgivendeOrgnumre.addAll(grunnlagData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere);

        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());
        arbeidsgivendeOrgnumre.retainAll(avklarteOrgnumreOgUuider);
        return arbeidsgivendeOrgnumre;
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendigeForetak(Behandling behandling) {
        return hentNorskeSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendigeForetak(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        Set<String> selvstendigeForetakOrgnumre = hentNorskeSelvstendigeForetakOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(selvstendigeForetakOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.SELVSTENDIG))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere(Behandling behandling) {
        return hentNorskeArbeidsgivere(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        Set<String> arbeidsgivendeOrgnumre = hentNorskeArbeidsgivendeOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(arbeidsgivendeOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.LOENNET_ARBEID))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling) {
        return hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) {
        List<AvklartVirksomhet> norskeVirksomheter = hentNorskeArbeidsgivere(behandling, adressekonverterer);
        norskeVirksomheter.addAll(hentNorskeSelvstendigeForetak(behandling, adressekonverterer));
        return norskeVirksomheter;
    }

    @Transactional
    public void lagreVirksomheterSomAvklartefakta(List<String> virksomhetIDer,
                                                  Long behandlingID) {
        validerVirksomhetIDerGyldige(virksomhetIDer, behandlingID);

        avklartefaktaService.slettAvklarteFakta(behandlingID, VIRKSOMHET);

        for (String virksomhetID : virksomhetIDer) {
            lagreVirksomhetSomAvklartfakta(virksomhetID, behandlingID);
        }
    }

    public void lagreVirksomhetSomAvklartfakta(String virksomhetID, Long behandlingID) {
        avklartefaktaService.leggTilAvklarteFakta(behandlingID, VIRKSOMHET, VIRKSOMHET.getKode(), virksomhetID, Avklartefakta.VALGT_FAKTA);
    }

    private void validerVirksomhetIDerGyldige(List<String> virksomhetIDer,
                                                 Long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        for (String virksomhetID : virksomhetIDer) {
            if (!erVirksomhetIDGyldig(virksomhetID, behandling)) {
                throw new FunksjonellException("VirksomhetID " + virksomhetID + " hører ikke til noen av arbeidsforholdene");
            }
        }
    }

    private boolean erVirksomhetIDGyldig(String virksomhetID, Behandling behandling) {
        BehandlingsgrunnlagData behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        return erVirksomhetForetakUtland(virksomhetID, behandlingsgrunnlagData)
            || erVirksomhetSelvstendigForetakEllerLagtInnManuelt(virksomhetID, behandlingsgrunnlagData)
            || erVirksomhetArbeidNorge(virksomhetID, behandling);
    }

    private boolean erVirksomhetForetakUtland(String uuid, BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return behandlingsgrunnlagData.hentUtenlandskeArbeidsgivereUuid().contains(uuid);
    }

    private boolean erVirksomhetSelvstendigForetakEllerLagtInnManuelt(String orgnr, BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return behandlingsgrunnlagData.hentAlleOrganisasjonsnumre().contains(orgnr);
    }

    private boolean erVirksomhetArbeidNorge(String orgnr, Behandling behandling) {
        return behandling.hentArbeidsforholdDokument().hentOrgnumre().contains(orgnr);
    }

    StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        if (adresse == null || StringUtils.isEmpty(adresse.postnummer)) {
            adresse = org.getOrganisasjonDetaljer().hentStrukturertPostadresse();
        }
        if (StringUtils.isEmpty(adresse.gatenavn)) {
            adresse.gatenavn = " ";
        }
        if (adresse.erNorsk()) {
            adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        } else if (StringUtils.isEmpty(adresse.postnummer)) {
            //Utenlandske adresser har ikke alltid postnummer
            adresse.postnummer = " ";
        }
        return adresse;
    }
}
