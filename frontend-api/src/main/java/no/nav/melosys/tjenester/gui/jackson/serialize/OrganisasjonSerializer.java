package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import org.springframework.util.StringUtils;

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
        organisasjonDto.setNavn(organisasjon.getNavnSammenslått());
        organisasjonDto.setOppstartdato(organisasjon.getOppstartsdato());
        if (!StringUtils.isEmpty(organisasjon.getEnhetstype())) {
            organisasjonDto.setOrganisasjonsform(kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype(), LocalDate.now()));
        }

        OrganisasjonsDetaljer detaljer = organisasjon.getOrganisasjonDetaljer();
        if (detaljer != null) {
            GeografiskAdresse forretningsadresse = detaljer.hentFørsteGyldigeForretningsadresse();
            organisasjonDto.setForretningsadresse(tilAdresseDto(forretningsadresse));

            GeografiskAdresse postadresse = detaljer.hentFørsteGyldigePostadresse();
            organisasjonDto.setPostadresse(tilAdresseDto(postadresse));
        }
        else {
            organisasjonDto.setForretningsadresse(tilAdresseDto(null));
            organisasjonDto.setPostadresse(tilAdresseDto(null));
        }
        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(GeografiskAdresse adresse) {
        AdresseDto dto = new AdresseDto();
        GateadresseDto gateadresse = new GateadresseDto();
        dto.setGateadresse(gateadresse);

        if (adresse == null) {
            // Tomt objekt til frontend (ikke null)
            return dto;
        }

        if (adresse instanceof SemistrukturertAdresse) {
            SemistrukturertAdresse sAdresse = (SemistrukturertAdresse) adresse;

            // FIXME Hvordan formaterer vi adresselinjer?
            StringBuilder stringBuilder = new StringBuilder();

            String linje1 = sAdresse.getAdresselinje1();
            stringBuilder.append(linje1 == null ? "" : linje1);
            String linje2 = sAdresse.getAdresselinje2();
            stringBuilder.append(linje2 == null ? "" : linje2);
            String linje3 = sAdresse.getAdresselinje3();
            stringBuilder.append(linje3 == null ? "" : linje3);

            String adresseLinje = stringBuilder.toString();
            adresseLinje.replaceAll("\\s+", " ");

            gateadresse.setGatenavn(adresseLinje);

            String postNummer = sAdresse.getPostnr();

            dto.setPostnr(postNummer);
            dto.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, postNummer, LocalDate.now()));
            dto.setLand(kodeverkService.dekod(FellesKodeverk.LANDKODERISO2, sAdresse.getLandkode(), LocalDate.now()));

            return  dto;
        } else if (adresse == null) {
            // Ingen gyldige adresser
            return dto;
        } else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw new RuntimeException("GeografiskAdresse ikke støttet " + adresse.getClass().getSimpleName());
        }
    }
}
