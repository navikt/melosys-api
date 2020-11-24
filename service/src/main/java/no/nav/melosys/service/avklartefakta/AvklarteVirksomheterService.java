package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.adresse.Adresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class AvklarteVirksomheterService {
    protected final AvklartefaktaService avklartefaktaService;
    protected final RegisterOppslagService registerOppslagService;
    protected final BehandlingService behandlingService;

    @Autowired
    public AvklarteVirksomheterService(AvklartefaktaService avklartefaktaService,
                                       RegisterOppslagService registerOppslagService,
                                       BehandlingService behandlingService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.behandlingService = behandlingService;
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

    public Set<String> hentNorskeArbeidsgivendeOrgnumre(Behandling behandling) throws TekniskException {
        ArbeidsforholdDokument arbDok = behandling.hentArbeidsforholdDokument();
        Set<String> arbeidsgivendeOrgnumre = arbDok.hentOrgnumre();
        BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        arbeidsgivendeOrgnumre.addAll(grunnlagData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere);

        Set<String> avklarteOrgnumreOgUuider = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId());
        arbeidsgivendeOrgnumre.retainAll(avklarteOrgnumreOgUuider);
        return arbeidsgivendeOrgnumre;
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendigeForetak(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer)
        throws IkkeFunnetException, TekniskException {
        Set<String> selvstendigeForetakOrgnumre = hentNorskeSelvstendigeForetakOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(selvstendigeForetakOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.SELVSTENDIG))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer)
        throws IkkeFunnetException, TekniskException {
        Set<String> arbeidsgivendeOrgnumre = hentNorskeArbeidsgivendeOrgnumre(behandling);
        return registerOppslagService.hentOrganisasjoner(arbeidsgivendeOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.LOENNET_ARBEID))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Behandling behandling, Function<OrganisasjonDokument, Adresse> adressekonverterer)
        throws IkkeFunnetException, TekniskException {
        List<AvklartVirksomhet> norskeVirksomheter = hentNorskeArbeidsgivere(behandling, adressekonverterer);
        norskeVirksomheter.addAll(hentNorskeSelvstendigeForetak(behandling, adressekonverterer));
        return norskeVirksomheter;
    }

    public void lagreVirksomheterSomAvklartefakta(List<String> virksomhetIDer, Long behandlingID) throws FunksjonellException, TekniskException {
        erVirksomhetIDerGyldig(virksomhetIDer, behandlingID);
        for (String virksomhetID : virksomhetIDer) {
            avklartefaktaService.leggTilAvklarteFakta(behandlingID, VIRKSOMHET, VIRKSOMHET.getKode(), virksomhetID, "TRUE");
        }
    }

    private boolean erVirksomhetIDerGyldig(List<String> virksomhetIDer, Long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        for (String virksomhetID : virksomhetIDer) {
            if (!erVirksomhetIDerGyldig(virksomhetID, behandling)) {
                throw new FunksjonellException("VirksomhetID " + virksomhetID + " hører ikke til noen av arbeidsforholdene");
            }
        }
        return true;
    }

    private boolean erVirksomhetIDerGyldig(String virksomhetID, Behandling behandling) throws TekniskException {
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

    private boolean erVirksomhetArbeidNorge(String orgnr, Behandling behandling) throws TekniskException {
        return behandling.hentArbeidsforholdDokument().hentOrgnumre().contains(orgnr);
    }
}