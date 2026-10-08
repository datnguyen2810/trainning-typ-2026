CREATE TABLE categories (
                            id UUID NOT NULL,
                            name VARCHAR(120) NOT NULL,
                            slug VARCHAR(150) NOT NULL,
                            description TEXT,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT pk_categories PRIMARY KEY (id),
                            CONSTRAINT uk_categories_slug UNIQUE (slug),
                            CONSTRAINT ck_categories_name_not_blank CHECK (BTRIM(name) <> ''),
                            CONSTRAINT ck_categories_slug_not_blank CHECK (BTRIM(slug) <> '')
);

CREATE TABLE products (
                          id UUID NOT NULL,
                          category_id UUID NOT NULL,
                          sku VARCHAR(80) NOT NULL,
                          name VARCHAR(200) NOT NULL,
                          slug VARCHAR(220) NOT NULL,
                          description TEXT,
                          price NUMERIC(19, 2) NOT NULL,
                          status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                          version BIGINT NOT NULL DEFAULT 0,
                          deleted_at TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT pk_products PRIMARY KEY (id),
                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id)
                                  REFERENCES categories (id)
                                  ON DELETE RESTRICT,
                          CONSTRAINT uk_products_sku UNIQUE (sku),
                          CONSTRAINT uk_products_slug UNIQUE (slug),
                          CONSTRAINT ck_products_sku_not_blank CHECK (BTRIM(sku) <> ''),
                          CONSTRAINT ck_products_name_not_blank CHECK (BTRIM(name) <> ''),
                          CONSTRAINT ck_products_slug_not_blank CHECK (BTRIM(slug) <> ''),
                          CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
                          CONSTRAINT ck_products_status
                              CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
                          CONSTRAINT ck_products_version_non_negative CHECK (version >= 0)
);

CREATE TABLE product_images (
                                id UUID NOT NULL,
                                product_id UUID NOT NULL,
                                url TEXT NOT NULL,
                                display_order INTEGER NOT NULL DEFAULT 0,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT pk_product_images PRIMARY KEY (id),
                                CONSTRAINT fk_product_images_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES products (id)
                                        ON DELETE CASCADE,
                                CONSTRAINT ck_product_images_url_not_blank CHECK (BTRIM(url) <> ''),
                                CONSTRAINT ck_product_images_display_order_non_negative
                                    CHECK (display_order >= 0),
                                CONSTRAINT uk_product_images_product_display_order
                                    UNIQUE (product_id, display_order)
);

