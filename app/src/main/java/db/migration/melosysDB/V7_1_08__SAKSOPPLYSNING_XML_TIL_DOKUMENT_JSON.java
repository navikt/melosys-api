package db.migration.melosysDB;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@SuppressWarnings("unused")
public class V7_1_08__SAKSOPPLYSNING_XML_TIL_DOKUMENT_JSON extends BaseJavaMigration {

    private final ObjectMapper objectMapper;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final TransformerFactory transformerFactory;
    private final Map<String, Transformer> transformerMap = new HashMap<>();

    public V7_1_08__SAKSOPPLYSNING_XML_TIL_DOKUMENT_JSON() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setPackagesToScan("no.nav.melosys.domain.dokument");
        jaxb2Marshaller.setValidationEventHandler(new DefaultValidationEventHandler());

        this.transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    @Override
    public void migrate(Context context) throws Exception {
        OracleConnection con = context.getConnection().unwrap(OracleConnection.class);
        con.setAutoCommit(false);
        OracleResultSet resultSet = null;
        try (Statement statement = con.createStatement()) {
            statement.setFetchSize(50);
            resultSet = (OracleResultSet) statement.executeQuery("SELECT * FROM SAKSOPPLYSNING");
            while (resultSet.next()) {
                opprettMigrering(resultSet, con);
            }
            con.commit();
        } finally {
            if (resultSet != null) resultSet.close();
        }
    }

    private void opprettMigrering(OracleResultSet resultSet, OracleConnection con) throws SQLException, TransformerException, JsonProcessingException {
        long saksopplysningID = resultSet.getLong("id");
        long behandlingID = resultSet.getLong("behandling_id");
        String versjon = resultSet.getString("versjon");
        String opplysningType = resultSet.getString("opplysning_type");
        String kildesystem = resultSet.getString("kilde");
        String xmlString = resultSet.getString("intern_xml");
        String dokumentJson = null;

        switch (opplysningType) {
            case "ARBFORH":
                dokumentJson = lagDokumentJson(
                    ArbeidsforholdDokument.class, xmlString,
                    "aareg/arbeidsforhold_"+ versjon + ".xslt", versjon);
                break;
            case "INNTK":
                dokumentJson =lagDokumentJson(
                    InntektDokument.class, xmlString, "inntk/inntekt_"+ versjon + ".xslt", versjon);
                break;
            case "ORG":
                dokumentJson = lagDokumentJson(
                    OrganisasjonDokument.class, xmlString, "ereg/organisasjon_"+ versjon + ".xslt", versjon);
                break;
            case "MEDL":
                dokumentJson =lagDokumentJson(
                    MedlemskapDokument.class, xmlString, "medl/medlemskap_"+ versjon + ".xslt", versjon);
                break;
            case "PERSHIST":
                dokumentJson = lagDokumentJson(
                    PersonhistorikkDokument.class, xmlString, "tps/personhistorikk_"+ versjon + ".xslt", versjon);
                break;
            case "PERSOPL":
                dokumentJson = lagDokumentJson(
                    PersonDokument.class, xmlString, "tps/person_"+ versjon + ".xslt", versjon);
                break;
            case "SEDOPPL":
                dokumentJson = lagDokumentJson(
                    SedDokument.class, xmlString, null, null);
                break;
            case "SOB_SAK":
                dokumentJson = lagDokumentJson(
                    SobSakDokument.class, xmlString, "sob/sakogbehandling_"+ versjon + ".xslt", versjon);
                break;
            case "UTBETAL":
                dokumentJson = lagDokumentJson(
                    UtbetalingDokument.class, xmlString, "utbetaling/utbetaldata_"+ versjon + ".xslt", versjon);
                break;
            default:
                throw new RuntimeException("Mapping fra saksopplysning type " + opplysningType + " ikke støttet");
        }
        if (dokumentJson != null) {
            oppdaterSaksopplysning(con, saksopplysningID, dokumentJson);
            opprettSaksopplysningKilde(con, saksopplysningID, kildesystem, xmlString);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SaksopplysningDokument> String lagDokumentJson(Class<T> clazz, String xml, String path, String versjon) throws TransformerException, JsonProcessingException {
        StringReader stringReader = clazz == SedDokument.class
            ? new StringReader(xml)
            : new StringReader(transformer(xml, path, versjon));
        T dokument = (T) jaxb2Marshaller.unmarshal(new StreamSource(stringReader));
        return objectMapper.writeValueAsString(dokument);
    }

    private String transformer(String xml, String path, String versjon) throws TransformerException {
        Transformer transformer;
        if (transformerMap.containsKey(path)) {
            transformer = transformerMap.get(path);
        } else {
            transformer = lagTransformer(path, versjon);
            transformerMap.put(path, transformer);
        }

        StreamResult outputTarget = new StreamResult(new StringWriter());
        StreamSource xmlSource = new StreamSource(new StringReader(xml));
        transformer.transform(xmlSource, outputTarget);
        return outputTarget.getWriter().toString();
    }

    private Transformer lagTransformer(String path, String versjon) throws TransformerConfigurationException {
        Transformer transformer = lagTemplate(path, versjon).newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        return transformer;
    }

    private Templates lagTemplate(String path, String versjon) throws TransformerConfigurationException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        return transformerFactory.newTemplates(new StreamSource(is));
    }

    private void oppdaterSaksopplysning(OracleConnection con, long saksopplysningID, String json) throws SQLException {
        try (OraclePreparedStatement ps = (OraclePreparedStatement) con.prepareStatement(
            "UPDATE SAKSOPPLYSNING SET dokument = ? WHERE id = ?")) {

            Clob clob = con.createClob();
            clob.setString(1, json);
            ps.setClob(1, clob);
            ps.setLong(2, saksopplysningID);

            ps.execute();
        }
    }

    private void opprettSaksopplysningKilde(OracleConnection con, long saksopplysningID, String kildesystem, String mottattDokument) throws SQLException {
        try (OraclePreparedStatement ps = (OraclePreparedStatement) con.prepareStatement(
            "INSERT INTO SAKSOPPLYSNING_KILDE(saksopplysning_id, kildesystem, mottatt_dokument) VALUES (?, ?, ?)")) {

            Clob clob = con.createClob();
            clob.setString(1, mottattDokument);
            ps.setLong(1, saksopplysningID);
            ps.setString(2, kildesystem);
            ps.setClob(3, clob);

            ps.execute();
        }

    }

    @Override
    public Integer getChecksum() {
        return 835_626_597;
    }
}