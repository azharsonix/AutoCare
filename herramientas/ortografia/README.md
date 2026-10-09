# Revisión ortográfica

Revisa la ortografía de los textos que ve el usuario en AutoCare (cadenas de
Kotlin y `strings.xml`) con el diccionario Hunspell de español de México, y
además busca tildes que faltan, preguntas o exclamaciones sin `¿` / `¡`,
espacios dobles y palabras repetidas.

## Requisitos

```
pip install spylls
```

El diccionario se descarga solo la primera vez en `diccionario/`.

## Uso

Desde la carpeta del proyecto:

```
python herramientas/ortografia/ortografia.py
python herramientas/ortografia/ortografia.py --comentarios   # también los comentarios del código
```

Termina con código 1 si encuentra algo en los textos de la app, así que sirve
como paso previo a publicar.

Si marca una palabra que sí es correcta (una marca, un término técnico),
agrégala a `palabras_permitidas.txt`.
