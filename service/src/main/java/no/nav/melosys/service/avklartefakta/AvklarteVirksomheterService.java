package no.nav.melosys.service.avklartefakta;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Yrkesaktivitetstyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentArbeidsforholdDokument;
import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentSøknadDokument;

@Service
@Primary
public class AvklarteVirksomheterService {
    protected final AvklartefaktaService avklartefaktaService;
    protected final RegisterOppslagService registerOppslagService;

    @Autowired
    public AvklarteVirksomheterService(AvklartefaktaService avklartefaktaService,
                                       RegisterOppslagService registerOppslagService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
    }

    public Set<String> hentSelvstendigeForetakOrgnumre(Behandling behandling) throws TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .collect(Collectors.toSet());

        Set<String> avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        organisasjonsnumre.retainAll(avklarteOrgnumre);
        return organisasjonsnumre;
    }

    public Set<String> hentArbeidsgivendeOrgnumre(Behandling behandling) throws TekniskException {
        ArbeidsforholdDokument arbDok = hentArbeidsforholdDokument(behandling);
        Set<String> arbeidsgivendeOrgnumre = arbDok.hentOrgnumre();
        SoeknadDokument søknad = hentSøknadDokument(behandling);
        arbeidsgivendeOrgnumre.addAll(søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere);

        Set<String> avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        arbeidsgivendeOrgnumre.retainAll(avklarteOrgnumre);
        return arbeidsgivendeOrgnumre;
    }

    public List<AvklartVirksomhet> hentSelvstendigeForetak(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Set<String> selvstendigeForetakOrgnumre = hentSelvstendigeForetakOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(selvstendigeForetakOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.SELVSTENDIG))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentArbeidsgivere(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Set<String> arbeidsgivendeOrgnumre = hentArbeidsgivendeOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(arbeidsgivendeOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.LOENNET_ARBEID))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<AvklartVirksomhet> norskeVirksomheter = hentArbeidsgivere(behandling, adressekonverterer);
        norskeVirksomheter.addAll(hentSelvstendigeForetak(behandling, adressekonverterer));
        return norskeVirksomheter;
    }
}