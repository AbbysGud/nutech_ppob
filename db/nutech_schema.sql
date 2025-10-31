
-- Nutech PPOB Assignment - Database Schema (MySQL 8.0)
-- Author: Ariq Bagus Sugiharto
-- Engine: InnoDB, Charset: utf8mb4
-- Money amounts are stored as BIGINT (IDR) to avoid floating point issues.

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

-- ---------------------------------------------------------------------------
-- TRIGGERS to apply balance changes atomically at insert time
-- Note: Application code should still wrap inserts in a transaction to avoid
-- race conditions. The triggers act as a final guardrail.
-- ---------------------------------------------------------------------------

DELIMITER $$

-- Before insert: compute balance_before/balance_after based on current wallet
CREATE TRIGGER bi_wallet_transactions
BEFORE INSERT ON wallet_transactions
FOR EACH ROW
BEGIN
  DECLARE current_balance BIGINT;
  -- Fetch current wallet balance with row-level lock via SELECT ... FOR UPDATE
  -- Note: MySQL does not support FOR UPDATE inside trigger. We will rely on
  -- application-side transaction with SELECT ... FOR UPDATE before the insert.
  SELECT balance INTO current_balance FROM wallets WHERE user_id = NEW.user_id;
  IF current_balance IS NULL THEN
    SET current_balance = 0;
  END IF;

  SET NEW.balance_before = current_balance;

  IF NEW.transaction_type = 'TOPUP' THEN
    SET NEW.balance_after = current_balance + NEW.amount;
    IF NEW.description IS NULL OR NEW.description = '' THEN
      SET NEW.description = 'Top Up balance';
    END IF;
  ELSEIF NEW.transaction_type = 'PAYMENT' THEN
    -- Fill snapshot if not provided
    IF NEW.service_name_snapshot IS NULL AND NEW.service_code IS NOT NULL THEN
      SELECT service_name INTO NEW.service_name_snapshot FROM services WHERE service_code = NEW.service_code;
    END IF;
    IF NEW.description IS NULL OR NEW.description = '' THEN
      IF NEW.service_name_snapshot IS NOT NULL THEN
        SET NEW.description = NEW.service_name_snapshot;
      ELSE
        SET NEW.description = 'Payment';
      END IF;
    END IF;
    -- Ensure sufficient balance
    IF current_balance < NEW.amount THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient balance';
    END IF;
    SET NEW.balance_after = current_balance - NEW.amount;
  END IF;
END$$

-- After insert: persist the new balance into wallets
CREATE TRIGGER ai_wallet_transactions
AFTER INSERT ON wallet_transactions
FOR EACH ROW
BEGIN
  UPDATE wallets
     SET balance = NEW.balance_after,
         version = version + 1,
         updated_at = CURRENT_TIMESTAMP
   WHERE user_id = NEW.user_id;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------------
-- SEEDERS (sample data) - adjust URLs as needed
-- ---------------------------------------------------------------------------

INSERT INTO banners (banner_name, banner_image_url, description, sort_order)
VALUES
 ('Banner 1', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 1),
 ('Banner 2', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 2),
 ('Banner 3', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 3),
 ('Banner 4', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 4),
 ('Banner 5', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 5),
 ('Banner 6', 'https://nutech-integrasi.app/dummy.jpg', 'Lorem Ipsum Dolor sit amet', 6);

INSERT INTO services (service_code, service_name, service_icon_url, service_tariff) VALUES
 ('PAJAK', 'Pajak PBB', 'https://nutech-integrasi.app/dummy.jpg', 40000),
 ('PLN', 'Listrik', 'https://nutech-integrasi.app/dummy.jpg', 10000),
 ('PDAM', 'PDAM Berlangganan', 'https://nutech-integrasi.app/dummy.jpg', 40000),
 ('PULSA', 'Pulsa', 'https://nutech-integrasi.app/dummy.jpg', 40000),
 ('PGN', 'PGN Berlangganan', 'https://nutech-integrasi.app/dummy.jpg', 50000),
 ('MUSIK', 'Musik Berlangganan', 'https://nutech-integrasi.app/dummy.jpg', 50000),
 ('TV', 'TV Berlangganan', 'https://nutech-integrasi.app/dummy.jpg', 50000),
 ('PAKET_DATA', 'Paket data', 'https://nutech-integrasi.app/dummy.jpg', 50000),
 ('VOUCHER_GAME', 'Voucher Game', 'https://nutech-integrasi.app/dummy.jpg', 100000),
 ('VOUCHER_MAKANAN', 'Voucher Makanan', 'https://nutech-integrasi.app/dummy.jpg', 100000),
 ('QURBAN', 'Qurban', 'https://nutech-integrasi.app/dummy.jpg', 200000),
 ('ZAKAT', 'Zakat', 'https://nutech-integrasi.app/dummy.jpg', 300000);

-- ---------------------------------------------------------------------------
-- OPTIONAL VIEW for transaction history formatting
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_transaction_history AS
SELECT
  t.user_id,
  t.invoice_number,
  t.transaction_type,
  t.description,
  t.amount AS total_amount,
  t.created_on
FROM wallet_transactions t
ORDER BY t.created_on DESC;
