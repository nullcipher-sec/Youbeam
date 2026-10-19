# YouBeam Project
Monorepo architecture for video streaming platform.

## Base de datos local

La base de datos usa PostgreSQL y se levanta mediante Docker Compose:

```bash
cd deploy
cp .env.example .env
docker compose up -d database
```

El esquema de `deploy/db_init/db.sql` se ejecuta automáticamente la primera vez
que se crea el volumen `youbeam_postgres_data`. Para comprobar el estado del
contenedor:

```bash
docker compose ps
docker compose logs database
```

Para conectarse desde el equipo local:

```bash
psql -h localhost -p 5433 -U youbeam -d youbeam
```

Si se modifica `db.sql` y se necesita ejecutarlo de nuevo desde cero, hay que
eliminar el volumen de desarrollo:

```bash
docker compose down -v
docker compose up -d database
```
