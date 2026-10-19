CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Tipos ENUM
CREATE TYPE user_status      AS ENUM ('active', 'suspended', 'deleted');
CREATE TYPE session_status   AS ENUM ('active', 'revoked', 'expired');
CREATE TYPE video_visibility AS ENUM ('public', 'hidden', 'private');
CREATE TYPE video_status     AS ENUM ('processing', 'ready', 'error');
CREATE TYPE asset_type       AS ENUM ('original', 'hls', 'thumbnail', 'encrypted_blob');

-- Usuarios (Actualizado a UUID por seguridad distribuida)
CREATE TABLE users (
                       id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                       username      VARCHAR(50)  UNIQUE NOT NULL,
                       email         VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name  VARCHAR(100),
                       status        user_status  DEFAULT 'active' NOT NULL,
                       created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       last_login_at TIMESTAMP
);

-- Sesiones (FK actualizadas a UUID)
CREATE TABLE sessions (
                          id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                          user_id       UUID REFERENCES users(id) ON DELETE CASCADE,
                          token_hash    VARCHAR(255) UNIQUE NOT NULL,
                          status        session_status DEFAULT 'active' NOT NULL,
                          ip_address    VARCHAR(45),
                          user_agent    VARCHAR(500),
                          expires_at    TIMESTAMP NOT NULL,
                          created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          revoked_at    TIMESTAMP
);

CREATE INDEX idx_sessions_token ON sessions(token_hash);
CREATE INDEX idx_sessions_user  ON sessions(user_id);

-- Roles (Catálogo interno, se mantiene numérico)
CREATE TABLE roles (
                       id          SMALLSERIAL PRIMARY KEY,
                       name        VARCHAR(30) UNIQUE NOT NULL,
                       description VARCHAR(255)
);

INSERT INTO roles (name, description) VALUES
                                          ('user',  'Usuario regular'),
                                          ('admin', 'Administrador')
    ON CONFLICT DO NOTHING;

-- Roles de Usuario
CREATE TABLE user_roles (
                            user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
                            role_id     SMALLINT REFERENCES roles(id) ON DELETE CASCADE,
                            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            PRIMARY KEY (user_id, role_id)
);

-- Categorías (Taxonomía, se mantiene numérico)
CREATE TABLE categories (
                            id         BIGSERIAL PRIMARY KEY,
                            name       VARCHAR(80) UNIQUE NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Videos (Añadido gen_random_uuid y FKs actualizadas)
CREATE TABLE videos (
                        id               UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                        owner_id         UUID REFERENCES users(id) ON DELETE CASCADE,
                        title            VARCHAR(200) NOT NULL,
                        description      TEXT,
                        visibility       video_visibility DEFAULT 'private' NOT NULL,
                        status           video_status     DEFAULT 'ready'   NOT NULL,
                        duration_seconds INT,
                        published_at     TIMESTAMP,
                        created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        deleted_at       TIMESTAMP
);

-- Índices Parciales Optimizados (Ignoran registros eliminados lógicamente)
CREATE INDEX idx_videos_active    ON videos(id) WHERE deleted_at IS NULL;
CREATE INDEX idx_videos_owner_vis ON videos(owner_id, visibility) WHERE deleted_at IS NULL;
CREATE INDEX idx_videos_public    ON videos(title) WHERE deleted_at IS NULL AND visibility = 'public';

-- Categorías de Videos
CREATE TABLE video_categories (
                                  video_id    UUID   REFERENCES videos(id) ON DELETE CASCADE,
                                  category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE,
                                  PRIMARY KEY (video_id, category_id)
);

-- Assets (Archivos multimedia dentro del Bucket Storage)
CREATE TABLE video_assets (
                              id               UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                              video_id         UUID REFERENCES videos(id) ON DELETE CASCADE,
                              asset_type       asset_type NOT NULL,
                              storage_bucket   VARCHAR(100) NOT NULL,
                              storage_key      VARCHAR(500) NOT NULL,
                              mime_type        VARCHAR(100) NOT NULL,
                              file_size_bytes  BIGINT NOT NULL,
                              checksum_sha256  CHAR(64) NOT NULL,
                              width            INT,
                              height           INT,
                              created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_assets_video ON video_assets(video_id);

-- Acceso Granular a Videos Privados/Ocultos
CREATE TABLE video_access (
                              video_id   UUID REFERENCES videos(id) ON DELETE CASCADE,
                              user_id    UUID REFERENCES users(id)  ON DELETE CASCADE,
                              granted_by UUID REFERENCES users(id),
                              granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                              expires_at TIMESTAMP,
                              PRIMARY KEY (video_id, user_id)
);

-- Reacciones (Likes y Dislikes)
CREATE TABLE video_reactions (
                                 video_id   UUID REFERENCES videos(id) ON DELETE CASCADE,
                                 user_id    UUID REFERENCES users(id)  ON DELETE CASCADE,
                                 value      SMALLINT NOT NULL CHECK (value IN (1, -1)),
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 PRIMARY KEY (video_id, user_id)
);

CREATE INDEX idx_reactions_video ON video_reactions(video_id);

-- NUEVO: Sistema Jerárquico de Comentarios
CREATE TABLE video_comments (
                                id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                                video_id   UUID REFERENCES videos(id) ON DELETE CASCADE,
                                user_id    UUID REFERENCES users(id)  ON DELETE CASCADE,
                                parent_id  UUID REFERENCES video_comments(id) ON DELETE CASCADE, -- Permite hilos de respuestas
                                content    TEXT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                deleted_at TIMESTAMP
);

CREATE INDEX idx_comments_video  ON video_comments(video_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_parent ON video_comments(parent_id);

CREATE TABLE video_views (
                             id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                             video_id   UUID REFERENCES videos(id) ON DELETE CASCADE,
                             user_id    UUID REFERENCES users(id)  ON DELETE SET NULL,
                             ip_address VARCHAR(45),
                             viewed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_views_video ON video_views(video_id);
CREATE INDEX idx_views_date  ON video_views(viewed_at);