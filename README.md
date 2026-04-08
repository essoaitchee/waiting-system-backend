# waiting-system-backend

## Database

The backend is configured for MySQL 8.

### Default connection properties

These values can be overridden with environment variables.

- `DB_HOST=localhost`
- `DB_PORT=3306`
- `DB_NAME=waiting_system`
- `DB_USERNAME=root`
- `DB_PASSWORD=1234`

JDBC URL:

```text
jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
```

### Schema and sample data

Run these files in order:

1. [`src/main/resources/sql/mysql-schema.sql`](./src/main/resources/sql/mysql-schema.sql)
2. [`src/main/resources/sql/mysql-sample-data.sql`](./src/main/resources/sql/mysql-sample-data.sql)
