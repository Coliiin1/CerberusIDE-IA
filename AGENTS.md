# AGENTS.md

## Maintenance requirement (fundamental)
- Update this file in the same change whenever an important modification is made:
  new/removed classes or packages, architecture or compile-flow changes, new or
  changed build/run commands, and any other non-obvious behavior an agent would
  otherwise get wrong. Do not leave it stale.

## Project
CerberusIDE — a JavaFX IDE/compiler for a custom Spanish-like pseudocode
language. Maven build; UI is JavaFX (FXML + CSS). Compiler logic is pure Java
(no Swing/JavaFX), so it can be tested headlessly.

## Build & run
- Maven project. Main launcher: `main.Main` -> `main.CerberusApp` (JavaFX `Application`).
- Maven Wrapper included — use `mvnw.cmd` (Windows) / `./mvnw` (unix); no global Maven
  install needed. It downloads Maven 3.9.16 to `~/.m2/wrapper/`.
- Run: `mvnw.cmd clean javafx:run`. Package: `mvnw.cmd clean package`.
- Requires JDK 24+. OpenJFX 24 is pulled from Maven (`pom.xml`, `javafx.version`).
  `maven.compiler.release = 24`, `sourceEncoding = UTF-8`.
- Local JavaFX SDK is at `C:\Users\danie\SKD\javafx-sdk-24\lib` (`PATH_TO_FX` env var).
  Run with it via `run.cmd`, or directly:
  `java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -cp target\classes main.Main`.
- Build output in `target/` (gitignored).

## Architecture
- `ui.InterfazPrincipalController` — JavaFX controller; compile flow lives in `compilar()`.
- `ui/principal.fxml` + `ui/estilos.css` — layout and light/dark themes
  (`.modo-oscuro` / `.modo-claro` looked-up colors in CSS).
- `AnalizadorLexico` — hand-written regex tokenizer (`Tokens`, `Token`, `Identificadores`).
- `AnalizadorSintactico` — recursive-descent parser; drives `Documentador`.
- `AnalizadorSemantico` — largely a stub; UI hardcodes its pass/fail text.
- `Archivos.Archivo` — file I/O (pure, no UI; returns booleans / throws `IOException`).
  `crear()` auto-appends `.txt`, `guardar()` does not; both write relative to the JVM
  working directory (not the opened file's folder).
- `Util.Reporter` — functional interface used by analyzers to surface errors to the UI
  (replaces the old `JOptionPane` coupling). Inject via `AnalizadorLexico#setReporter`
  or the `AnalizadorSintactico` constructor.

## Language & testing
- Spanish keywords only (`clase`, `principal`, `funcion`, `imprimir`, `si`, `sino`,
  `para`, `mientras`, `segun`, `retornar`, ...). Sample programs: root `*.txt` files.
- No test suite, and no lint/typecheck step. The only verification is compiling
  (`mvnw.cmd compile` or `mvnw.cmd clean package`).
- Errors surface via JavaFX `Alert` dialogs (and `Reporter`), not throwable output.
- Resources are under `src/main/resources` (`Imagenes/`, `ui/`); loaded from the classpath.
