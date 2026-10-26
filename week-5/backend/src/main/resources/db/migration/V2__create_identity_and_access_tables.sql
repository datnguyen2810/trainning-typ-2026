CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_email_not_blank CHECK (BTRIM(email) <> ''),
    CONSTRAINT ck_users_email_normalized CHECK (email = LOWER(BTRIM(email))),
    CONSTRAINT ck_users_username_format CHECK (username ~ '^[a-z0-9_.]{3,50}$'),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (BTRIM(password_hash) <> ''),
    CONSTRAINT ck_users_full_name_not_blank CHECK (BTRIM(full_name) <> ''),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

-- System reference data: roles and permissions intentionally have no timestamps.
CREATE TABLE roles (
    id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code_format CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_roles_name_not_blank CHECK (BTRIM(name) <> '')
);

CREATE TABLE permissions (
    id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    description VARCHAR(500),

    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code),
    CONSTRAINT ck_permissions_code_format CHECK (code ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id)
        REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    CONSTRAINT ck_refresh_tokens_hash_not_blank CHECK (BTRIM(token_hash) <> ''),
    CONSTRAINT ck_refresh_tokens_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_tokens_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT ck_refresh_tokens_replacement_not_self CHECK (replaced_by_token_id <> id),
    CONSTRAINT ck_refresh_tokens_replacement_revoked
        CHECK (replaced_by_token_id IS NULL OR revoked_at IS NOT NULL)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_replaced_by_token_id ON refresh_tokens (replaced_by_token_id);

-- Stable identifiers keep reference data reproducible across environments.
-- User accounts and production credentials are not seeded here.
INSERT INTO roles (id, code, name, description)
VALUES
    ('10000000-0000-4000-8000-000000000001', 'CUSTOMER', 'Customer', 'Customer account'),
    ('10000000-0000-4000-8000-000000000002', 'ADMIN', 'Administrator', 'System administrator');

INSERT INTO permissions (id, code, description)
VALUES
    ('20000000-0000-4000-8000-000000000001', 'CATALOG_MANAGE', 'Create and manage categories and products'),
    ('20000000-0000-4000-8000-000000000002', 'INVENTORY_READ', 'Read inventory quantities and transactions'),
    ('20000000-0000-4000-8000-000000000003', 'INVENTORY_MANAGE', 'Import and adjust inventory'),
    ('20000000-0000-4000-8000-000000000004', 'VOUCHER_MANAGE', 'Create and manage vouchers'),
    ('20000000-0000-4000-8000-000000000005', 'ORDER_READ_ALL', 'Read orders across customers'),
    ('20000000-0000-4000-8000-000000000006', 'ORDER_UPDATE', 'Update order processing status'),
    ('20000000-0000-4000-8000-000000000007', 'USER_MANAGE', 'Manage users and role assignments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';
