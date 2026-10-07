# RUTA.md — Bitácora del proyecto CerberusAI

**Propósito:** bitácora viva del avance del proyecto. Se **consulta y actualiza en cada paso** para tener un trazo claro del trabajo realizado y del que sigue.

> **Regla:** al completar cada paso, marcar el estado (`PENDIENTE` → `EN CURSO` → `HECHO`) y anotar el entregable. No dejar este archivo obsoleto.

**Estados:** `HECHO` · `EN CURSO` · `PENDIENTE`

---

## Ruta seguida

| # | Paso | Estado | Entregable |
|---|---|---|---|
| 1 | Reconstruir contexto del proyecto | HECHO | análisis de `CerberusAI_CONTEXT.md` + código fuente |
| 2 | Auditar el compilador (estático + dinámico) | HECHO | `src/main/java/audit/CompilerProbe.java`, `CerberusAI_COMPILER_AUDIT.md` |
| 3 | Reconciliar `language_spec.md` con el compilador real | HECHO | `CerberusAI/knowledge/language_spec.md` (sección 26.5 de conflictos) |
| 4 | Crear suite de tests ejecutable | HECHO | `tests/valid/`, `tests/invalid/`, `tests/expected/invalid/`, `src/main/java/audit/TestSuite.java` (97/97 PASS) |
| 5 | Inventario de gramáticas incompletas | HECHO | `GramaticasAttendance.md` |
| 6 | Implementar gramáticas prioridad ALTA | HECHO | 6.1–6.9 completados; tests en `tests/` (97/97 PASS) |
| 7 | Re-auditar y re-ejecutar suite | HECHO | `CerberusAI_COMPILER_AUDIT.md` re-auditado; `TestSuite` 99/99, `CompilerProbe` 48/48; fix UI semántico |
| 8 | Actualizar `language_spec.md` con lo implementado | HECHO | spec consolidada y re-auditada (`CerberusAI/knowledge/language_spec.md`) |
| 9 | Construir `cerberus_dataset_v2.jsonl` | HECHO | `CerberusAI/dataset/cerberus_dataset_v2.jsonl` (93 entradas; 45 programas `generation` validados contra el compilador) |
| 10 | Evaluación automática + elección de modelo base local | PENDIENTE | — |
| 11 | Servicio Python/FastAPI | PENDIENTE | — |
| 12 | Integración con el IDE JavaFX | PENDIENTE | — |

---

## Detalle del paso 6 — gramáticas ALTA (orden por dependencias)

| # | Feature | Ref. `GramaticasAttendance.md` | Estado |
|---|---|---|---|
| 6.1 | Scopes reales | 2.12 | HECHO |
| 6.2 | Validación de tipos | 2.11 | HECHO |
| 6.3 | Booleano: inicialización + condición | 2.4, 2.5 | HECHO |
| 6.4 | Negación `!` | 2.6 | HECHO |
| 6.5 | Asignación compuesta `+= -= *= /=` | 2.7 | HECHO |
| 6.6 | `segun` con `predeterminado` | 2.10 | HECHO |
| 6.7 | `retornar` + tipo de retorno | 2.2, 2.3 | HECHO |
| 6.8 | Parámetros de funciones | 2.1 | HECHO |
| 6.9 | `para` generalizado | 2.9 | HECHO |

Cada sub-paso se valida añadiendo casos a `tests/` y ejecutando `TestSuite` + `CompilerProbe`.

---

## Detalle del paso 6-bis — gramáticas MEDIA (orden por dependencias) — HECHO

Plan completo en `GramaticasAttendance.md` (sección 5). Misma fase de pruebas que ALTA
(`mvnw.cmd -q clean compile` + `audit.TestSuite` + `audit.CompilerProbe`, verdes antes de cerrar).

| # | Feature | Ref. `GramaticasAttendance.md` | Estado |
|---|---|---|---|
| M1 | Errores tipados (`ErrorSintactico`) | 2.16 | HECHO |
| M2 | Llamada a funciones | 2.13 | HECHO |
| M3 | `imprimir` multi-argumento | 2.14 | HECHO |
| M4 | `nulo` / `vacio` (alcance) | 2.15 | HECHO |

Decisiones de alcance: M2 = sentencia y expresión (valor no evaluado); M3 = `,` + booleanos + llamadas; M4 = `nulo` reservado/fuera de v1 (`vacio` ya HECHO).

---

## Detalle del paso 6-ter — cierre de alcance BAJA (2.17/2.18) — HECHO

Fuera de v1 (ver sección 3 de `GramaticasAttendance.md`). El parser rechaza POO y arreglos de
forma explícita: `nuevo`/`este`/`publico`/`privado` → `POO no implementada en v1`; `x.y` →
`POO no implementada en v1: acceso a miembro`; `entero [] a;` → `Arreglos no implementados en v1`.
Verificado por inválidos `15`, `43`, `44` y la sonda (`99/99` suite, `48/48` sonda).

---

## Artefactos del proyecto

- Compilador: `src/main/java/AnalizadorLexico`, `AnalizadorSintactico`, `AnalizadorSemantico`.
- Sonda: `src/main/java/audit/CompilerProbe.java`.
- Suite: `src/main/java/audit/TestSuite.java` + `tests/`.
- Auditoría: `CerberusAI_COMPILER_AUDIT.md`.
- Guía de gramáticas: `GramaticasAttendance.md`.
- Especificación: `CerberusAI/knowledge/language_spec.md`.
- Dataset: `CerberusAI/dataset/cerberus_dataset_v1.jsonl`.

## Decisiones tomadas

- **POO avanzada fuera de v1**: herencia, constructores, arreglos completos y `publico`/`privado` reales se posponen; se documentan como futuras (`PENDIENTE`) y no generan ejemplos en el dataset v2.
- **`nulo` reservado (fuera de v1)**: sin tipos por referencia, `nulo` se documenta como `DOCUMENTED/PENDING`; `vacio` sí se usa como tipo de retorno (2.2). No genera ejemplos en el dataset v2.
- El compilador real es la fuente de verdad; los conflictos con el PDF se registran en `CerberusAI_COMPILER_AUDIT.md` (sección de conflictos) y en `language_spec.md` (26.5).

## Comandos de verificación

```cmd
mvnw.cmd compile
java -cp target\classes audit.TestSuite
java -cp target\classes audit.CompilerProbe
```
