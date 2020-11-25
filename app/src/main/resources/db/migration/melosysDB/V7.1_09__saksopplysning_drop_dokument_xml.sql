ALTER TABLE saksopplysning DROP (
    kilde,
    dokument_xml,
    intern_xml
);

ALTER TABLE saksopplysning MODIFY (
    dokument NOT NULL
);
