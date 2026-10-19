-- V1__catalogo.sql
-- Catálogo de datasets y sus columnas (PostgreSQL 16)

CREATE TABLE datasets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL
                  CONSTRAINT ck_datasets_name_not_blank CHECK (btrim(name) <> ''),
    description   TEXT NOT NULL,
    -- FK a users(id) se añade en la migración de seguridad (V2)
    owner_id      UUID NOT NULL,
    format        VARCHAR(10) NOT NULL
                  CONSTRAINT ck_datasets_format CHECK (format IN ('CSV')),
    -- Versión codificada: major * 1000000 + minor * 1000 + patch
    -- minor y patch entre 0 y 999 se valida en el dominio, no aquí
    version       INTEGER NOT NULL
                  CONSTRAINT ck_datasets_version_non_negative CHECK (version >= 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    lock_version  INTEGER NOT NULL DEFAULT 0
);

-- Nombre único sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_datasets_name_lower ON datasets (lower(name));


CREATE TABLE dataset_columns (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id     UUID NOT NULL REFERENCES datasets (id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL
                   CONSTRAINT ck_dataset_columns_name_not_blank CHECK (btrim(name) <> ''),
    position       INTEGER NOT NULL
                   CONSTRAINT ck_dataset_columns_position_positive CHECK (position >= 1),
    expected_type  VARCHAR(20) NOT NULL
                   CONSTRAINT ck_dataset_columns_expected_type
                   CHECK (expected_type IN ('STRING', 'INTEGER', 'FLOAT', 'BOOLEAN', 'DATE')),
    required       BOOLEAN NOT NULL,

    CONSTRAINT uq_dataset_columns_position UNIQUE (dataset_id, position)
);

-- Nombre único dentro de cada dataset, sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_dataset_columns_name ON dataset_columns (dataset_id, lower(name));