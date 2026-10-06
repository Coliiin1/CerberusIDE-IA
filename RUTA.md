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
| 4 | Crear suite de tests ejecutable | HECHO | `tests/valid/`, `tests/invalid/`, `tests/expected/invalid/`, `src/main/java/audit/TestSuite.java` (32/32 PASS) |
| 5 | Inventario de gramáticas incompletas | HECHO | `GramaticasAttendance.md` |
| 6 | Implementar gramáticas prioridad ALTA | EN CURSO | (ver detalle en paso 6) |
| 7 | Re-auditar y re-ejecutar suite | PENDIENTE | actualizar `CerberusAI_COMPILER_AUDIT.md` |
| 8 | Actualizar `language_spec.md` con lo implementado | PENDIENTE | — |
| 9 | Construir `cerberus_dataset_v2.jsonl` | PENDIENTE | `CerberusAI/dataset/cerberus_dataset_v2.jsonl` |
| 10 | Evaluación automática + elección de modelo base local | PENDIENTE | — |
| 11 | Servicio Python/FastAPI | PENDIENTE | — |
| 12 | Integración con el IDE JavaFX | PENDIENTE | — |

---

## Detalle del paso 6 — gramáticas ALTA (orden por dependencias)

| # | Feature | Ref. `GramaticasAttendance.md` | Estado |
|---|---|---|---|
| 6.1 | Scopes reales | 2.12 | HECHO |
| 6.2 | Validación de tipos | 2.11 | PENDIENTE |
| 6.3 | Booleano: inicialización + condición | 2.4, 2.5 | PENDIENTE |
| 6.4 | Negación `!` | 2.6 | PENDIENTE |
| 6.5 | Asignación compuesta `+= -= *= /=` | 2.7 | PENDIENTE |
| 6.6 | `segun` con `predeterminado` | 2.10 | PENDIENTE |
| 6.7 | `retornar` + tipo de retorno | 2.2, 2.3 | PENDIENTE |
| 6.8 | Parámetros de funciones | 2.1 | PENDIENTE |
| 6.9 | `para` generalizado | 2.9 | PENDIENTE |

Cada sub-paso se valida añadiendo casos a `tests/` y ejecutando `TestSuite` + `CompilerProbe`.

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
- El compilador real es la fuente de verdad; los conflictos con el PDF se registran en `CerberusAI_COMPILER_AUDIT.md` (sección de conflictos) y en `language_spec.md` (26.5).

## Comandos de verificación

```cmd
mvnw.cmd compile
java -cp target\classes audit.TestSuite
java -cp target\classes audit.CompilerProbe
```
