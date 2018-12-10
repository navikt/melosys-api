CREATE TABLE tidligere_medlemsperiode (
    behandling_id   NUMBER(19) NOT NULL,
    periode_id      NUMBER(19) NOT NULL,
    CONSTRAINT pk_medlemsperiode_behandling PRIMARY KEY (behandling_id, periode_id),
    CONSTRAINT fk_medlemsperiode_behandling FOREIGN KEY (behandling_id) REFERENCES behandling
);

CREATE INDEX idx_medlemsperiode_behandling ON tidligere_medlemsperiode (behandling_id);