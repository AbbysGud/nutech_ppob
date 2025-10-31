# Nutech PPOB API — Take Home Test

Implementasi REST API sesuai **Documentation for Take Home Test API** (Nutech Integrasi).
Kontrak respons, pesan, dan struktur field diikuti **persis** sesuai dokumen (termasuk wording dan urutan field).

## 🔧 Tech Stack
- Java 21 / Spring Boot 3.4
- Spring Security (JWT, Bearer)
- Raw SQL via `JdbcTemplate` (prepared statements, tanpa JPA/Hibernate)
- MySQL 8 (lokal / Docker)
- Testcontainers (integrasi test) / H2 fallback (opsional)
- VS Code REST Client / Postman Collection

---

## 📦 Struktur Proyek (ringkas)

```
.
├─ src
│  ├─ main
│  │  ├─ java/.../config
│  │  ├─ java/.../controller
│  │  ├─ java/.../dto
│  │  ├─ java/.../exception
│  │  ├─ java/.../repo
│  │  ├─ java/.../service
│  │  ├─ java/.../util
│  │  └─ resources
│  └─ test
│     ├─ java/.../test
│     └─ resources
├─ tests
│  └─ api.http
├─ schema.sql
├─ seed.sql
├─ Dockerfile
├─ docker-compose.yml
├─ .gitignore
└─ README.md
```

---

## 🚀 Cara Menjalankan

### Opsi A — **Paling cepat**: Full Docker (App + MySQL)
1. Jalankan stack:
   ```bash
   docker compose up -d --build
   ```
2. Cek status:
   ```bash
   docker compose ps
   # Harus terlihat:
   # ppob-mysql  ...  Healthy
   # ppob-app    ...  Up (healthy)
   ```
3. Uji cepat (endpoint publik):
   ```bash
   mysql -h 127.0.0.1 -P 3307 -u ppob_user -pppob_pass
   ```
4. Perintah berguna (opsional):
   ```bash
   # lihat log service
   docker compose logs -f db
   docker compose logs -f app

   # hentikan stack
   docker compose down

   # reset total (hapus volume DB & ulang init schema/seed)
   docker compose down -v
   docker compose up -d --build
   ```

### Opsi B — **Tanpa Docker**: MySQL sudah terpasang
1. Buat DB & user:
   ```sql
   CREATE DATABASE ppob;
   CREATE USER 'ppob_user'@'%' IDENTIFIED BY 'ppob_pass';
   GRANT ALL ON ppob.* TO 'ppob_user'@'%';
   FLUSH PRIVILEGES;
   ```
2. Import `schema.sql` (+ `seed.sql` opsional).
3. Atur `application.yml` atau gunakan variabel env seperti Opsi A.
4. Jalankan `./mvnw spring-boot:run`.

> Aplikasi tidak memakai JPA; semua query adalah **raw SQL** dengan **prepared statements**.

---

## 🔐 Environment Variables

| Key               | Contoh           | Keterangan                                     |
|-------------------|------------------|-----------------------------------------------|
| APP_JWT_SECRET    | change-this      | Secret untuk signing JWT                      |
| APP_JWT_EXP_HOURS | 12               | Expiration JWT (jam)                          |
| DB_HOST           | 127.0.0.1        | Host MySQL                                    |
| DB_PORT           | 3306             | Port MySQL                                    |
| DB_NAME           | ppob             | Nama database                                 |
| DB_USER           | ppob_user        | User DB                                       |
| DB_PASS           | ppob_pass        | Password DB                                   |

---

## 🔒 Security & Error Mapping
- Endpoint **private** memakai **JWT Bearer**.
- **401 (JWT invalid/expired)** → `status:108`.
- **401 (login salah)** → `status:103`.
- **400 (validasi & bisnis)** → `status:102` + pesan sesuai dokumen.

---

## 🔁 Transaksi DB & Raw Query
- Pembayaran memakai `SELECT balance FOR UPDATE` untuk locking saldo.
- Insert ledger ke `wallet_transactions` dan update `wallets.balance` dalam satu transaksi.
- Semua query ditulis sebagai **prepared statements**.

---

## 🔗 Endpoint Ringkas + Contoh `curl`

**1) Registrasi**
```bash
curl -X POST http://localhost:8080/registration -H "Content-Type: application/json" -d '{ "email":"user@nutech-integrasi.com", "password":"abcdef1234", "firstName":"User", "lastName":"Nutech" }'
```

**2) Login**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{ "email":"user@nutech-integrasi.com", "password":"abcdef1234" }' | jq -r '.data.token')
```

**3) Get Profile**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/profile
```

**4) Update Profile**
```bash
curl -X PUT http://localhost:8080/profile/update -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{ "firstName":"Ariq B.", "lastName":"Sugiharto" }'
```

**5) Update Profile Image**
```bash
curl -X PUT http://localhost:8080/profile/image -H "Authorization: Bearer $TOKEN" -H "Content-Type: multipart/form-data" -F "file=@/path/ke/foto.jpg"
```

**6) Get Balance**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/balance
```

**7) Topup**
```bash
curl -X POST http://localhost:8080/topup -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{ "amount": 100000 }'
```

**8) Payment**
```bash
curl -X POST http://localhost:8080/transaction -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{ "service_code": "PULSA" }'
```

**9) History**
```bash
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/transaction/history?offset=0&limit=3"
```

**10) Public Banner**
```bash
curl http://localhost:8080/banner
```

**11) Services (Private)**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/services
```

---

## 🧪 Testing

- Integration Test (Testcontainers):
  ```bash
  mvn -Dspring.profiles.active=test test
  ```
- VS Code REST Client:
  Buka `tests/api.http`, klik **Send Request**.

---

## 📄 Lisensi

MIT License — bebas digunakan untuk keperluan belajar atau evaluasi.
