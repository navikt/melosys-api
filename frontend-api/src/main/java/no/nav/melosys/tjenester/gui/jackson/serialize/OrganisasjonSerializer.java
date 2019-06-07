package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import org.apache.commons.lang3.StringUtils;

public class OrganisasjonSerializer extends StdSerializer<OrganisasjonDokument> {

    private final KodeverkService kodeverkService;

    public OrganisasjonSerializer(KodeverkService kodeverkService) {
        super(OrganisasjonDokument.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(OrganisasjonDokument organisasjon, JsonGenerator generator, SerializerProvider provider) throws IOException {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();

        organisasjonDto.setOrgnr(organisasjon.getOrgnummer());
        organisasjonDto.setNavn(organisasjon.lagSammenslåttNavn());
        organisasjonDto.setOppstartdato(organisasjon.getOppstartsdato());
        if (StringUtils.isNotEmpty(organisasjon.getEnhetstype())) {
            organisasjonDto.setOrganisasjonsform(kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype(), LocalDate.now()));
        }

        OrganisasjonsDetaljer detaljer = organisasjon.getOrganisasjonDetaljer();
        if (detaljer != null) {
            StrukturertAdresse forretningsadresse = detaljer.hentStrukturertForretningsadresse();
            organisasjonDto.setForretningsadresse(tilAdresseDto(forretningsadresse));

            StrukturertAdresse postadresse = detaljer.hentStrukturertPostadresse();
            organisasjonDto.setPostadresse(tilAdresseDto(postadresse));
        }
        else {
            organisasjonDto.setForretningsadresse(tilAdresseDto(null));
            organisasjonDto.setPostadresse(tilAdresseDto(null));
        }
        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(StrukturertAdresse adresse) {
        AdresseDto dto = new AdresseDto();
        GateadresseDto gateadresse = new GateadresseDto();
        dto.setGateadresse(gateadresse);

        if (adresse == null) {
            // Tomt objekt til frontend (ikke null)
            return dto;
        }

        gateadresse.setGatenavn(adresse.gatenavn);

        dto.setPostnr(adresse.postnummer);
        String poststed = StringUtils.isNotEmpty(adresse.poststed) ? adresse.poststed : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        dto.setPoststed(poststed);
        dto.setLand(kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, adresse.landkode, LocalDate.now()));

        return  dto;
    }
}
