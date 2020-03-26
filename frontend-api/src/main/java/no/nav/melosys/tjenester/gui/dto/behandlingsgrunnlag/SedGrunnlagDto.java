package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.Organisasjon;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;

public class SedGrunnlagDto extends BehandlingsgrunnlagData {
    public List<String> overgangsregelbestemmelser;
    public List<OrganisasjonDto> norskeArbeidsgivere;
    public String ytterligereInformasjon;

    public SedGrunnlagDto(SedGrunnlag sedGrunnlag) {
        overgangsregelbestemmelser = sedGrunnlag.hentOvergangsregelbestemmelsekoder();
        norskeArbeidsgivere = sedGrunnlag.norskeArbeidsgivere.stream().map(this::lagOrganisasjonDto).collect(Collectors.toList());
        ytterligereInformasjon = sedGrunnlag.ytterligereInformasjon;
    }

    private OrganisasjonDto lagOrganisasjonDto(Organisasjon organisasjon) {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();
        organisasjonDto.setOrgnr(organisasjon.getOrgnr());
        organisasjonDto.setNavn(organisasjon.getNavn());
        organisasjonDto.setPostadresse(new AdresseDto(organisasjon.getPostadresse()));
        organisasjonDto.setForretningsadresse(new AdresseDto(organisasjon.getForretningsadresse()));
        return organisasjonDto;
    }
}
