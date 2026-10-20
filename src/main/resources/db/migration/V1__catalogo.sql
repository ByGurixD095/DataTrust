CREATE TABLE datasets (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL
                       CONSTRAINT ck_datasets_name_not_blank CHECK (btrim(name) <> ''),
    description        TEXT NOT NULL,
    owner_id           UUID NOT NULL,
    current_version_id UUID NOT NULL,
    lock_version       INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_datasets_name_lower ON datasets (lower(name));

CREATE TABLE dataset_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id      UUID NOT NULL REFERENCES datasets (id) ON DELETE CASCADE,
    version_number  VARCHAR(32) NOT NULL,
    schema_snapshot JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID NOT NULL,
    CONSTRAINT uq_dataset_versions_number UNIQUE (dataset_id, version_number)
);

ALTER TABLE datasets
    ADD CONSTRAINT fk_datasets_current_version
    FOREIGN KEY (current_version_id) REFERENCES dataset_versions (id)
    DEFERRABLE INITIALLY DEFERRED;