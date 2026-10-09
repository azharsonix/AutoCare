"""
Revisión ortográfica de AutoCare.

Revisa los textos que ve el usuario (cadenas de Kotlin y strings.xml) y, con
--comentarios, también los comentarios del código. Reglas:
  1. Diccionario Hunspell de español de México (el de LibreOffice).
  2. Tildes que faltan en palabras que sin ella no existen (vehiculo -> vehículo).
  3. Preguntas y exclamaciones sin el signo de apertura (¿ ¡).
  4. Espacios dobles, espacio antes de la puntuación y palabras repetidas.

Uso (desde cualquier carpeta):
    python herramientas/ortografia/ortografia.py               # solo textos de la app
    python herramientas/ortografia/ortografia.py --comentarios # también comentarios

Devuelve código de salida 1 si encuentra algo en los textos de la app, para
poder usarlo antes de publicar o en integración continua.
"""
import argparse
import re
import sys
import unicodedata
import urllib.request
from functools import lru_cache
from pathlib import Path

try:
    from spylls.hunspell import Dictionary
except ImportError:
    sys.exit("Falta la librería spylls. Instálala con:  pip install spylls")

CARPETA = Path(__file__).resolve().parent
PROYECTO = CARPETA.parent.parent
CARPETA_DICCIONARIO = CARPETA / "diccionario"
URL_DICCIONARIO = "https://raw.githubusercontent.com/wooorm/dictionaries/main/dictionaries/es-MX/index"


def cargar_diccionario() -> Dictionary:
    """Descarga el diccionario es-MX la primera vez y lo deja en herramientas/ortografia/diccionario."""
    CARPETA_DICCIONARIO.mkdir(exist_ok=True)
    for extension in ("aff", "dic"):
        destino = CARPETA_DICCIONARIO / f"es_MX.{extension}"
        if not destino.exists():
            print(f"Descargando diccionario ({extension})…", file=sys.stderr)
            urllib.request.urlretrieve(f"{URL_DICCIONARIO}.{extension}", destino)
    return Dictionary.from_files(str(CARPETA_DICCIONARIO / "es_MX"))


def cargar_lista_blanca() -> set[str]:
    """Una palabra por línea; las líneas con # son comentarios."""
    archivo = CARPETA / "palabras_permitidas.txt"
    if not archivo.exists():
        return set()
    return {
        linea.strip().lower()
        for linea in archivo.read_text(encoding="utf-8").splitlines()
        if linea.strip() and not linea.lstrip().startswith("#")
    }


DICCIONARIO = cargar_diccionario()
LISTA_BLANCA = cargar_lista_blanca()

# Palabras que sin tilde no existen (o casi nunca se usan sin ella en esta app).
TILDES_OBLIGATORIAS = {
    "vehiculo": "vehículo", "vehiculos": "vehículos", "dia": "día", "dias": "días",
    "telefono": "teléfono", "numero": "número", "aqui": "aquí", "despues": "después",
    "tambien": "también", "ultimo": "último", "ultima": "última", "proximo": "próximo",
    "proxima": "próxima", "proximos": "próximos", "categoria": "categoría", "categorias": "categorías",
    "contrasena": "contraseña", "electronico": "electrónico", "sesion": "sesión",
    "informacion": "información", "accion": "acción", "edicion": "edición", "poliza": "póliza",
    "circulacion": "circulación", "verificacion": "verificación", "afinacion": "afinación",
    "rotacion": "rotación", "anio": "año", "facil": "fácil", "rapido": "rápido", "codigo": "código",
    "pagina": "página", "mecanico": "mecánico", "vacio": "vacío", "credito": "crédito",
    "galeria": "galería", "camara": "cámara", "fotografia": "fotografía", "titulo": "título",
    "notificacion": "notificación", "kilometro": "kilómetro", "kilometros": "kilómetros",
}

# Al empezar una pregunta llevan tilde.
INTERROGATIVAS = {
    "que": "qué", "como": "cómo", "cuanto": "cuánto", "cuanta": "cuánta", "cuantos": "cuántos",
    "cuantas": "cuántas", "donde": "dónde", "cuando": "cuándo", "quien": "quién", "quienes": "quiénes",
    "cual": "cuál", "cuales": "cuáles",
}

# Pronombres que se pegan al verbo ("actualízalo", "tómale").
ENCLITICOS = ("selos", "selas", "sela", "selo", "melo", "telo", "los", "las", "les", "nos", "lo", "la", "le", "me", "te", "se")

PALABRA = re.compile(r"[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+(?:-[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+)*")
CADENA = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
PLANTILLA = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_.]*")
SQL = re.compile(r"\b(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|VALUES|INTEGER|PRIMARY KEY|NOT NULL)\b")
IDENTIFICADOR = re.compile(
    r"\b[A-Za-z0-9]*_[A-Za-z0-9_]*\b"           # CON_GUIONES_BAJOS
    r"|\b[a-z]+[A-Z][A-Za-z0-9]*\b"             # camelCase
    r"|\b[A-Z][a-z0-9]+[A-Z][A-Za-z0-9]*\b"     # PascalCase con varias mayúsculas
)


def sin_acentos(texto: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn")


@lru_cache(maxsize=None)
def palabra_valida(palabra: str) -> bool:
    minuscula = palabra.lower()
    if minuscula in LISTA_BLANCA or len(minuscula) <= 1:
        return True
    if palabra.isupper() and len(palabra) <= 8:  # siglas: MXN, PDF, CASCADE
        return True
    if DICCIONARIO.lookup(palabra) or DICCIONARIO.lookup(minuscula) or DICCIONARIO.lookup(minuscula.capitalize()):
        return True
    # Verbo con pronombre pegado: se quita el pronombre y la tilde que este obliga a poner.
    for enclitico in ENCLITICOS:
        if minuscula.endswith(enclitico) and len(minuscula) > len(enclitico) + 2:
            base = minuscula[: -len(enclitico)]
            if DICCIONARIO.lookup(base) or DICCIONARIO.lookup(sin_acentos(base)):
                return True
    return False


# --- Extracción -------------------------------------------------------------

def parece_texto_de_usuario(cadena: str) -> bool:
    """Se queda con frases; descarta SQL, rutas de navegación, claves y formatos."""
    limpio = PLANTILLA.sub("X", cadena).strip()
    if not re.search(r"[A-Za-zÁÉÍÓÚáéíóúñÑ]{2,}", limpio):
        return False
    if SQL.search(limpio):
        return False
    if " " not in limpio and re.search(r"[/=?{}@]", limpio):  # rutas y correos: "vehicles/new", "a@b.com"
        return False
    if " " not in limpio and (limpio.startswith("X") or re.search(r"\d", limpio)):  # claves: "${id}-dia", "PBKDF2…"
        return False
    if re.fullmatch(r"[a-z][A-Za-z0-9_\-.]*", limpio):      # claves e identificadores
        return False
    if re.search(r"%[,.\d]*[dfs]", limpio) or re.fullmatch(r"[A-Za-z/ ,.\-']+", limpio) and re.search(r"\b(EEEE?|MMMM|yyyy|dd|MM)\b", limpio):
        return False
    return True


def extraer_kotlin(ruta: Path):
    textos, comentarios = [], []
    en_bloque = False
    for n, linea in enumerate(ruta.read_text(encoding="utf-8").splitlines(), 1):
        quitar = linea.strip()
        if quitar.startswith(("import ", "package ")):
            continue
        if en_bloque or quitar.startswith(("/*", "*")):
            comentarios.append((n, re.sub(r"^\s*(/\*\*?|\*/?)", "", linea)))
            en_bloque = "*/" not in linea
            continue
        codigo = linea
        # Un "//" fuera de comillas abre un comentario de línea.
        posicion = next((m.start() for m in re.finditer(r"//", linea) if linea.count('"', 0, m.start()) % 2 == 0), None)
        if posicion is not None:
            comentarios.append((n, linea[posicion + 2:]))
            codigo = linea[:posicion]
        for cadena in CADENA.findall(codigo):
            if parece_texto_de_usuario(cadena):
                textos.append((n, cadena.replace('\\"', '"').replace("\\n", "\n")))
    return textos, comentarios


def extraer_xml(ruta: Path):
    textos = []
    for n, linea in enumerate(ruta.read_text(encoding="utf-8").splitlines(), 1):
        textos.extend((n, valor) for valor in re.findall(r"<string[^>]*>(.*?)</string>", linea))
    return textos


# --- Reglas -----------------------------------------------------------------

def revisar(texto: str, es_comentario: bool = False):
    hallazgos = []
    limpio = PLANTILLA.sub("X", texto)
    limpio = re.sub(r"\S+@\S+|https?://\S+", "X", limpio)  # correos y direcciones web
    if es_comentario:
        limpio = re.sub(r'"[^"]*"', "X", limpio)  # en comentarios, lo entrecomillado suele ser código
    # En comentarios, [referencia] y `código` no son texto; tampoco los identificadores.
    limpio = re.sub(r"\[[^\]]*\]|`[^`]*`", "X", limpio)
    limpio = IDENTIFICADOR.sub("X", limpio)

    for palabra in PALABRA.findall(limpio):
        correccion = TILDES_OBLIGATORIAS.get(palabra.lower())
        if correccion:
            hallazgos.append(("tilde", palabra, f"debe ser «{correccion}»"))
        elif not palabra_valida(palabra):
            hallazgos.append(("desconocida", palabra, "no está en el diccionario"))

    for m in re.finditer(r"(?:^|[.!?]\s+|\n)([^¿.!?\n]*\?)", limpio):
        frase = m.group(1).strip()
        if "¿" not in frase and re.search(r"[a-záéíóúñ]{3}", frase):
            hallazgos.append(("signo", frase, "falta «¿» de apertura"))
    # Palabra interrogativa al empezar una pregunta: lleva tilde (¿Cuántos…?, ¿Qué…?).
    for m in re.finditer(r"(?:^|[.!?]\s+|¿)\s*([A-Za-záéíóúñ]+)[^.!?¿]*\?", limpio):
        palabra = m.group(1)
        if palabra.lower() in INTERROGATIVAS:
            hallazgos.append(("tilde", palabra, f"en pregunta debe ser «{INTERROGATIVAS[palabra.lower()]}»"))
    for m in re.finditer(r"(?:^|[.?]\s+)([^¡.!?\n]*!)", limpio):
        frase = m.group(1).strip()
        if frase and "¡" not in frase:
            hallazgos.append(("signo", frase, "falta «¡» de apertura"))
    if re.search(r"\S {2,}\S", limpio):
        hallazgos.append(("espacio", texto, "espacio doble"))
    if re.search(r"\w\s+[,.;:](?!\.)", limpio):
        hallazgos.append(("espacio", texto, "espacio antes de la puntuación"))
    for m in re.finditer(r"\b([A-Za-zÁÉÍÓÚáéíóúñÑ]{2,})\s+\1\b", limpio, re.IGNORECASE):
        hallazgos.append(("repetida", m.group(0), "palabra repetida"))
    return hallazgos


def main():
    argumentos = argparse.ArgumentParser(description="Revisión ortográfica de AutoCare")
    argumentos.add_argument("--comentarios", action="store_true", help="revisar también los comentarios del código")
    argumentos.add_argument("--proyecto", type=Path, default=PROYECTO, help="raíz del proyecto (por defecto, esta)")
    opciones = argumentos.parse_args()
    raiz = opciones.proyecto.resolve()

    textos, comentarios, revisados = [], [], 0
    for ruta in sorted((raiz / "app/src").rglob("*.kt")):
        if ruta.name.startswith("Example"):  # plantillas en inglés de Android Studio
            continue
        relativa = ruta.relative_to(raiz).as_posix()
        en_textos, en_comentarios = extraer_kotlin(ruta)
        revisados += len(en_textos)
        textos += [(relativa, n, *h) for n, t in en_textos for h in revisar(t)]
        if opciones.comentarios:
            comentarios += [(relativa, n, *h) for n, t in en_comentarios for h in revisar(t, es_comentario=True)]
    for ruta in (raiz / "app/src/main/res").rglob("strings.xml"):
        relativa = ruta.relative_to(raiz).as_posix()
        en_textos = extraer_xml(ruta)
        revisados += len(en_textos)
        textos += [(relativa, n, *h) for n, t in en_textos for h in revisar(t)]

    secciones = [("Textos de la app", textos)] + ([("Comentarios", comentarios)] if opciones.comentarios else [])
    for titulo, hallazgos in secciones:
        print(f"\n== {titulo}: {len(hallazgos)} por revisar ==")
        for archivo, n, tipo, palabra, detalle in hallazgos:
            print(f"{archivo}:{n}  [{tipo}]  {palabra!r}  {detalle}")
    print(f"\nTextos revisados: {revisados}.")
    if not textos:
        print("Sin errores en los textos de la app.")
    else:
        print("Si una palabra es correcta, agrégala a herramientas/ortografia/palabras_permitidas.txt.")
    sys.exit(1 if textos else 0)


if __name__ == "__main__":
    main()
