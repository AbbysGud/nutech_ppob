-- Pastikan berada di database yang benar
-- USE your_database_name;

-- Buang trigger lama jika ada
DROP TRIGGER IF EXISTS bi_wallet_transactions;
DROP TRIGGER IF EXISTS ai_wallet_transactions;

-- Trigger BEFORE INSERT: hitung balance_before, balance_after, default description,
-- dan validasi saldo cukup untuk PAYMENT
DELIMITER $$

CREATE TRIGGER bi_wallet_transactions
BEFORE INSERT ON wallet_transactions
FOR EACH ROW
BEGIN
  DECLARE current_balance BIGINT DEFAULT 0;
  DECLARE svc_name VARCHAR(100);

  -- Ambil saldo saat ini dari wallets
  SELECT COALESCE(balance, 0)
    INTO current_balance
    FROM wallets
   WHERE user_id = NEW.user_id
   LIMIT 1;

  -- Set saldo sebelum transaksi
  SET NEW.balance_before = current_balance;

  IF NEW.transaction_type = 'TOPUP' THEN
    -- Tambah saldo untuk topup
    SET NEW.balance_after = current_balance + COALESCE(NEW.amount, 0);

    -- Default description jika kosong
    IF NEW.description IS NULL OR NEW.description = '' THEN
      SET NEW.description = 'Top Up balance';
    END IF;

  ELSEIF NEW.transaction_type = 'PAYMENT' THEN
    -- Pastikan ada nama layanan bila belum diisi di snapshot
    IF (NEW.service_name_snapshot IS NULL OR NEW.service_name_snapshot = '')
       AND NEW.service_code IS NOT NULL THEN
      SELECT service_name
        INTO svc_name
        FROM services
       WHERE service_code = NEW.service_code
       LIMIT 1;

      SET NEW.service_name_snapshot = svc_name;
    END IF;

    -- Default description Payment
    IF NEW.description IS NULL OR NEW.description = '' THEN
      SET NEW.description = COALESCE(NEW.service_name_snapshot, 'Payment');
    END IF;

    -- Validasi saldo cukup
    IF current_balance < COALESCE(NEW.amount, 0) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient balance';
    END IF;

    -- Kurangi saldo untuk payment
    SET NEW.balance_after = current_balance - COALESCE(NEW.amount, 0);
  END IF;

  -- Jika kolom created_on ada dan ingin set otomatis
  -- SET NEW.created_on = CURRENT_TIMESTAMP;
END$$

DELIMITER ;

-- Trigger AFTER INSERT: sinkronkan tabel wallets dari nilai balance_after
DELIMITER $$

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
