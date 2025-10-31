## Database & DDL

- DB: MySQL 8 (Laragon)
- Nama DB: `nutech_db`

### Import
```bash
# tanpa password
mysql -u root -h 127.0.0.1 -P 3306 nutech_db < db/nutech_schema.sql

# dengan password
mysql -u root -p -h 127.0.0.1 -P 3306 nutech_db < db/nutech_schema.sql