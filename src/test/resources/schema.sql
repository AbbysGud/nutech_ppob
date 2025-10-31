DROP TABLE IF EXISTS wallet_transactions;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS banners;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------------
-- Database (optional; uncomment if you want to create a dedicated database)
-- ---------------------------------------------------------------------------
-- CREATE DATABASE IF NOT EXISTS nutech_ppob CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- USE nutech_ppob;

-- ---------------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email            VARCHAR(191)     NOT NULL,
  first_name       VARCHAR(100)     NOT NULL,
  last_name        VARCHAR(100)     NOT NULL,
  password_hash    CHAR(60)         NOT NULL, -- e.g., bcrypt
  profile_image_url VARCHAR(512)    DEFAULT NULL,
  last_login_at    DATETIME         DEFAULT NULL,
  created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- WALLETS (1:1 with users)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallets (
  user_id          BIGINT UNSIGNED  NOT NULL,
  balance          BIGINT           NOT NULL DEFAULT 0, -- IDR
  version          INT              NOT NULL DEFAULT 0, -- optional for optimistic locking
  created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- BANNERS (public, read-only via API)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS banners (
  id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  banner_name      VARCHAR(150)     NOT NULL,
  banner_image_url VARCHAR(512)     NOT NULL,
  description      VARCHAR(500)     DEFAULT NULL,
  sort_order       INT              NOT NULL DEFAULT 0,
  is_active        TINYINT(1)       NOT NULL DEFAULT 1,
  created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_banners_active_order (is_active, sort_order)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- SERVICES (private, read-only via API)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS services (
  service_code     VARCHAR(64)      NOT NULL, -- e.g., "PULSA", "PLN"
  service_name     VARCHAR(150)     NOT NULL,
  service_icon_url VARCHAR(512)     NOT NULL,
  service_tariff   BIGINT           NOT NULL, -- IDR
  is_active        TINYINT(1)       NOT NULL DEFAULT 1,
  created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (service_code)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- TRANSACTIONS (ledger)
-- Each row is an immutable entry of a balance change (TOPUP or PAYMENT).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transactions (
  id                     BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  invoice_number         VARCHAR(64)      NOT NULL,
  user_id                BIGINT UNSIGNED  NOT NULL,
  service_code           VARCHAR(64)      DEFAULT NULL, -- NULL for TOPUP
  service_name_snapshot  VARCHAR(150)     DEFAULT NULL, -- snapshot for audit/history
  transaction_type       ENUM('TOPUP','PAYMENT') NOT NULL,
  description            VARCHAR(255)     NOT NULL,
  amount                 BIGINT           NOT NULL,  -- positive value in IDR
  balance_before         BIGINT           NOT NULL,
  balance_after          BIGINT           NOT NULL,
  created_on             DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_invoice_number (invoice_number),
  KEY idx_tx_user_created (user_id, created_on DESC),
  KEY idx_tx_user_type (user_id, transaction_type),
  CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_tx_service FOREIGN KEY (service_code) REFERENCES services(service_code) ON UPDATE CASCADE,
  CONSTRAINT chk_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_balance_nonneg CHECK (balance_after >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_hist_user_created ON wallet_transactions(user_id, created_on DESC);