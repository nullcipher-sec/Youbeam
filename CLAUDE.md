# YouBeam — reglas para Claude

Plataforma de video con cifrado de extremo a extremo (proyecto de Seguridad Informática).
Arquitectura y flujo de equipo: ver `README.md`. Guía de seguridad: `docs/GUIA-SEGURIDAD.md`.

## 1. Consultar graphify primero

Antes de leer o buscar archivos para responder preguntas sobre el código, la arquitectura
o la relación entre componentes:

1. Si existe `graphify-out/graph.json`, usar la skill **graphify** y consultar el grafo
   (`graphify query "<pregunta>"`, `graphify path "A" "B"`, `graphify explain "X"`).
   Leer `graphify-out/GRAPH_REPORT.md` para el panorama general.
2. Solo después, abrir los archivos concretos que el grafo señale.
3. Si `graphify-out/` no existe, avisar y proponer generarlo con `/graphify`.
4. Tras cambios grandes en el código, actualizar el grafo con `/graphify . --update`.

## 2. Commits y Pull Requests

- **Claude no aparece como autor ni colaborador**: nunca añadir `Co-Authored-By: Claude`,
  `Generated with Claude Code` ni ninguna otra atribución a Claude/Anthropic en commits o PRs.
  (Reforzado por `.claude/settings.json` y el hook `.claude/hooks/block-claude-attribution.sh`.)
- Formato Conventional Commits en inglés: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`.
- Commits pequeños y enfocados.
- Solo hacer commit o push cuando el usuario lo pida.

## 3. Ramas

- Cada integrante trabaja en `dev/<usuario>`; **nunca** push directo a `main`.
- Integración vía Pull Request hacia `main`, previo `git merge origin/main` en la rama propia.

## 4. Seguridad

- Nunca subir ni mostrar el contenido de `.env`, `deploy/secrets/` ni claves privadas.
- Solo `.env.example` se versiona.
- No debilitar el cifrado (AES-256-GCM, JWT ES256, DTLS-SRTP) ni desactivar validaciones
  para "hacer que funcione"; si algo bloquea, explicarlo y proponer la alternativa segura.

## 5. Idioma

- Responder al usuario en español.
- Código, identificadores, mensajes de commit y mensajes de log/error en inglés.
- **Todos los comentarios en español**: Javadoc, `//`, `#` en `.properties`, `Dockerfile`,
  `.gitignore`, scripts, etc.
- Configuración de Spring Boot en `application.properties` (no YAML).
- `youbeam_core` sigue la estructura de Airtek Channels: paquetes `Interface`, `service`,
  `controller`, `repository`, `entity`, `model/request`, `model/response`, `exception`,
  `config`, `commons`, `enums`, `util`; perfiles `dev`/`prod`; errores con `ControllerException`
  + catálogo `ErrorResponses` + `ExceptionControllerAdvice`.

## 6. Tareas (issues de GitHub)

- Las tareas de cada integrante están en los issues de `nullcipher-sec/Youbeam`
  (las de @marcelopcx: issue #1). Trabajarlas en orden, una a la vez.
- Al terminar una tarea, marcar su casilla (`- [x]`) en el issue con `gh issue edit`.
- Después de cada tarea, responder siempre con:
  1. **Qué hice**: archivos creados/modificados.
  2. **Cómo funciona**: explicación del diseño y de las decisiones de seguridad.
  3. **Cómo probarlo**: comandos concretos y resultado esperado.
