#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/5] Verificando catálogo, migración y referencias del frontend..."
python3 - <<'PY'
from pathlib import Path
import csv
import re

root = Path('.')
catalog = root / 'backend/src/main/resources/data/iso27001_controls.csv'
with catalog.open(encoding='utf-8-sig', newline='') as handle:
    rows = list(csv.DictReader(handle, delimiter='|'))
codes = [row['codigo'].strip() for row in rows]
assert len(rows) == 93, f'El catálogo contiene {len(rows)} filas y se esperaban 93'
assert len(set(codes)) == 93, 'Existen códigos duplicados en el catálogo'

migration = (root / 'database/migrations/2026_08_rpm_soa_formacion_integral.sql').read_text(encoding='utf-8')
migration_codes = re.findall(
    r"VALUES \('([^']+)'\s*,\s*'(?:Organizacional|Personas|Físico|Tecnológico)'",
    migration,
)
assert len(migration_codes) == 93, f'La migración contiene {len(migration_codes)} controles y se esperaban 93'
assert set(migration_codes) == set(codes), 'El catálogo CSV y la migración no tienen los mismos códigos'

expected_tables = {
    'catalogo_controles', 'perfiles_organizacionales', 'soa_controles',
    'riesgos', 'riesgos_controles', 'evidencias', 'hallazgos_auditoria',
    'modulos_capacitacion', 'participantes_capacitacion', 'preguntas_capacitacion',
    'intentos_capacitacion', 'rpm_analisis', 'rpm_senales', 'rpm_decisiones',
    'rpm_memoria',
}
created_tables = set(re.findall(r'CREATE TABLE IF NOT EXISTS\s+([a-zA-Z0-9_]+)', migration, flags=re.I))
missing_tables = expected_tables - created_tables
assert not missing_tables, f'Faltan tablas en la migración: {sorted(missing_tables)}'

pages = {page.stem: page for page in (root / 'frontend/pages').glob('*.html')}
for script in (root / 'frontend/js').glob('*.js'):
    page = pages.get(script.stem)
    if page is None:
        continue
    html = page.read_text(encoding='utf-8')
    code = script.read_text(encoding='utf-8')
    html_ids = set(re.findall(r'id=["\']([^"\']+)', html))
    referenced_ids = set(re.findall(r'getElementById\(["\']([^"\']+)', code))
    missing_ids = referenced_ids - html_ids
    assert not missing_ids, f'{script.name} referencia IDs inexistentes: {sorted(missing_ids)}'

    handlers = set(re.findall(r'on(?:click|change|submit|input)=["\']([A-Za-z_$][\w$]*)\s*\(', html))
    functions = set(re.findall(
        r'(?:function\s+|(?:const|let|var)\s+)([A-Za-z_$][\w$]*)\s*(?:=\s*)?(?:async\s*)?\(',
        code,
    ))
    missing_handlers = handlers - functions
    assert not missing_handlers, f'{script.name} no define handlers: {sorted(missing_handlers)}'

print('  OK: 93 controles únicos, migración sincronizada y frontend referencialmente consistente')
PY

echo "[2/5] Verificando sintaxis JavaScript..."
if command -v node >/dev/null 2>&1; then
  for file in frontend/js/*.js; do
    node --check "$file" >/dev/null
  done
  echo "  OK: sintaxis de JavaScript"
else
  echo "  AVISO: Node.js no está instalado; se omite esta comprobación"
fi

echo "[3/5] Compilando fuentes Java..."
if ! command -v javac >/dev/null 2>&1; then
  echo "  ERROR: se requiere JDK 20 o 21 para validar el backend" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
DEPS_DIR="$TMP_DIR/deps"
CLASSES_DIR="$TMP_DIR/classes"
mkdir -p "$DEPS_DIR" "$CLASSES_DIR"
JAR_FILE="backend/target/backend-0.0.1-SNAPSHOT.jar"

if [[ -f "$JAR_FILE" ]]; then
  (cd "$DEPS_DIR" && jar xf "$ROOT_DIR/$JAR_FILE")
  CLASSPATH="$(find "$DEPS_DIR/BOOT-INF/lib" -name '*.jar' -printf '%p:' | sed 's/:$//')"
  find backend/src/main/java -name '*.java' > "$TMP_DIR/sources.txt"
  javac -encoding UTF-8 --release 20 -parameters -cp "$CLASSPATH" -d "$CLASSES_DIR" @"$TMP_DIR/sources.txt"
else
  echo "  No existe el JAR de dependencias; se intentará compilar con Maven Wrapper"
  (cd backend && ./mvnw -B -DskipTests package)
fi

echo "  OK: fuentes Java compiladas"

echo "[4/5] Verificando integración ML..."
python3 - <<'PYML'
from pathlib import Path
root=Path('.')
required=[
 root/'ml-service/app/main.py',
 root/'ml-service/model/modelo_rpm_rf_humano_v1.joblib',
 root/'ml-service/Dockerfile.render',
 root/'database/migrations/2026_08_rpm_ml_hibrido_tidb.sql',
 root/'backend/src/main/java/com/globalisosecurity/backend/services/RpmMlPredictionService.java',
]
missing=[str(x) for x in required if not x.exists()]
assert not missing, f'Faltan artefactos ML: {missing}'
print('  OK: microservicio ML, modelo, migración y cliente Spring presentes')
PYML

echo "[5/5] Resumen..."
echo "  Controles: 93"
echo "  Tablas integrales verificadas: 15"
echo "  Páginas por rol: administrador, implementador, auditor, capacitador y empresa"
echo "  Integración híbrida: RPM determinista + Random Forest experimental + validación humana"
echo "Validación estática completada correctamente. Las pruebas E2E requieren MySQL/Docker."
