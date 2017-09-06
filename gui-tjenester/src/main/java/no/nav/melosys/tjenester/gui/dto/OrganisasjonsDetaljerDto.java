package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.tjenester.gui.dto.util.AdresseUtils;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.GeografiskAdresse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;

// FIXME Avklare hva skal brukes fra EREG. Mangler opplysninger.
public class OrganisasjonsDetaljerDto {

    private BostedsadresseDto forretningsadresse;

    private String postadresse;

    public OrganisasjonsDetaljerDto() {
    }

    public static OrganisasjonsDetaljerDto toDto(OrganisasjonsDetaljer organisasjonDetaljer) {
        OrganisasjonsDetaljerDto dto = new OrganisasjonsDetaljerDto();

        List<GeografiskAdresse> forretningsadresser = organisasjonDetaljer.getForretningsadresse();
        for (GeografiskAdresse adresse : forretningsadresser) {
            // TODO hvis det finnes flere gyldige adresser?
            if (AdresseUtils.erGyldig(adresse)) {
                dto.setForretningsadresse(BostedsadresseDto.lagForretningsadresse(adresse));
                break;
            }
        }

        List<GeografiskAdresse> postadresser = organisasjonDetaljer.getPostadresse();
        for (GeografiskAdresse adresse : postadresser) {
            if (AdresseUtils.erGyldig(adresse)) {
                dto.setPostadresse(AdresseUtils.lagPostadresse(adresse));
                break;
            }
        }

        return dto;
    }

    public BostedsadresseDto getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(BostedsadresseDto forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }

    public String getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(String postadresse) {
        this.postadresse = postadresse;
    }







}