# Archivo

Si en el servidor funciona el módulo **Sammlungen**, **Ver › Archivo** (Ctrl+9) o el icono **Archivo** abre su archivo en una ventana propia. El [árbol en este PC](hilfe:lokal) ya incluye el módulo. El archivo guarda más que el árbol: fotos y documentos no tienen que estar vinculados a nadie. Quién puede ver y cambiar qué lo decide webtrees con sus permisos de usuario.

## Disposición

- **A la izquierda** las colecciones: carpetas del archivo, colecciones temáticas, colecciones por tipo de medio y los medios no vinculados a ninguna persona. Debajo se indica cuántos archivos pertenecen al fondo libre, es decir, a los que nada del árbol hace referencia. Es intencionado, no un defecto.
- **En el centro** los archivos de la colección elegida en cuadrícula, con tres tamaños a elegir arriba. La etiqueta de la imagen indica el número de personas vinculadas, **GEDCOM** significa objeto multimedia sin persona, **libre** significa solo archivo.
- **A la derecha** los datos del archivo elegido: descripción, fecha, personas y palabras clave del archivo, colecciones, tamaño, ruta y quién está vinculado a él en el árbol. Un clic en un nombre convierte a esa persona en central.

Arrastre los separadores para ajustar el ancho de las columnas; wtWin lo recuerda.

## Manejo

Un clic selecciona un archivo, **Ctrl+clic** añade más, **Mayús+clic** todo un rango, **Ctrl+A** todos. Con las flechas, **Inicio** y **Fin** se recorre la cuadrícula; **Intro** o doble clic abre: imágenes en el visor, PDF en el visor de PDF, todo lo demás en el navegador.

El **clic derecho** ofrece: Abrir, Editar datos, Copiar ruta (también Ctrl+C), Guardar copias en una carpeta y Abrir en webtrees. Guardar copias trae los originales a este PC; los archivos existentes no se sobrescriben.

## Añadir archivos

Quien puede subir medios en webtrees añade archivos con **Añadir archivos …** o los arrastra desde el explorador a la ventana. Carpeta y colección temática vienen preseleccionadas con la colección abierta. Descripción, fecha, personas y palabras clave son opcionales y solo se escriben en imágenes (XMP).

wtWin sube el original y no reduce nada. Un archivo más grande de lo que acepta el servidor se omite y se indica. El límite lo fija el servidor (PHP `upload_max_filesize`). Después, los archivos no están vinculados a nadie. Para vincular uno a una persona, una fuente o un lugar, use **Medio existente › Del archivo** en la pestaña **Multimedia**.

## Editar datos

Los administradores del árbol cambian descripción, fecha, personas y palabras clave de una imagen con el clic derecho, el botón de la derecha o el lápiz del visor. El módulo escribe los datos en el propio archivo.

## Copia de seguridad

En el árbol en este PC, **Archivo › Copia de seguridad del árbol y el archivo …** guarda todo en un archivo ZIP: la base de datos con árboles y colecciones y toda la carpeta de medios, incluido el fondo libre. Un archivo GEDCOM o GEDZIP solo incluye los archivos a los que el árbol hace referencia. Para restaurar, cierre wtWin y coloque la carpeta `data` del archivo ZIP en la carpeta de webtrees (dónde está se explica en [árbol en este PC](hilfe:lokal)).

En un servidor, la copia la hace el propio servidor, por ejemplo nas4webtrees o su proveedor de alojamiento.
