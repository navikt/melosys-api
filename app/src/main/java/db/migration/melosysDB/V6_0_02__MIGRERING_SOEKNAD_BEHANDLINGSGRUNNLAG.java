package db.migration.melosysDB;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@SuppressWarnings("unused")
public class V6_0_02__MIGRERING_SOEKNAD_BEHANDLINGSGRUNNLAG extends BaseJavaMigration {

    private final ObjectMapper objectMapper;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final TransformerFactory transformerFactory;
    private final Map<String, Transformer> transformerMap = new HashMap<>();

    public V6_0_02__MIGRERING_SOEKNAD_BEHANDLINGSGRUNNLAG() {
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
            resultSet = (OracleResultSet) statement.executeQuery("SELECT * FROM SAKSOPPLYSNING WHERE OPPLYSNING_TYPE = 'SØKNAD'");
            while (resultSet.next()) {
                opprettBehandlingsgrunnlag(resultSet, con);
            }

            con.commit();
        } finally {
            if (resultSet != null) resultSet.close();
        }
    }

    private void opprettBehandlingsgrunnlag(OracleResultSet resultSet, OracleConnection con) throws Exception {
        long behandlingID = resultSet.getLong("behandling_id");
        String versjon = resultSet.getString("versjon");
        Instant registrertDato = resultSet.getTimestamp("registrert_dato").toInstant();
        Instant endretDato = resultSet.getTimestamp("endret_dato").toInstant();
        String xmlString = resultSet.getString("intern_xml");
        String søknadJson = lagSøknadDokumentJson(xmlString, versjon);

        insertSøknad(con, behandlingID, versjon, registrertDato, endretDato, søknadJson);
    }

    private String lagSøknadDokumentJson(String søknadXml, String versjon) throws JsonProcessingException, TransformerException {
        StringReader stringReader = new StringReader(transformer(søknadXml, versjon));
        Soeknad soeknad = (Soeknad) jaxb2Marshaller.unmarshal(new StreamSource(stringReader));
        return objectMapper.writeValueAsString(soeknad);
    }

    private String transformer(String søknadXml, String versjon) throws TransformerException {
        Transformer transformer;
        if (transformerMap.containsKey(versjon)) {
            transformer = transformerMap.get(versjon);
        } else {
            transformer = lagTransformer(versjon);
            transformerMap.put(versjon, transformer);
        }

        StreamResult outputTarget = new StreamResult(new StringWriter());
        StreamSource xmlSource = new StreamSource(new StringReader(søknadXml));
        transformer.transform(xmlSource, outputTarget);
        return outputTarget.getWriter().toString();
    }

    private Transformer lagTransformer(String versjon) throws TransformerConfigurationException {
        Transformer transformer = lagTemplate(versjon).newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        return transformer;
    }

    private Templates lagTemplate(String versjon) throws TransformerConfigurationException {
        String path = "soeknad/soeknad_"+ versjon + ".xslt";
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        return transformerFactory.newTemplates(new StreamSource(is));
    }

    private void insertSøknad(OracleConnection con, long behandlingID, String versjon, Instant registrertDato, Instant endretDato, String søknadJson) throws SQLException {
        try (OraclePreparedStatement ps = (OraclePreparedStatement) con.prepareStatement(
            "INSERT INTO BEHANDLINGSGRUNNLAG(behandling_id, versjon, registrert_dato, endret_dato, type, data) VALUES (?, ?, ?, ?, 'SØKNAD', ?)")) {

            Clob clob = con.createClob();
            clob.setString(1, søknadJson);

            ps.setLong(1, behandlingID);
            ps.setString(2, versjon);
            ps.setTimestamp(3, Timestamp.from(registrertDato));
            ps.setTimestamp(4, Timestamp.from(endretDato));
            ps.setClob(5, clob);

            ps.execute();
        }
    }

    @Override
    public Integer getChecksum() {
        return 1_764_893_572;
    }
}
