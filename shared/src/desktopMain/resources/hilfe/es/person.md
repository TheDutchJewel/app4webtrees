# Ficha personal y edición

La ficha personal se abre con un **doble clic** en una persona, con **Ctrl+E** o con el icono **Editar**. Muestra todo sobre una persona y es también el lugar para editar.

## Estructura

Arriba el nombre, las fechas y la imagen. Debajo, las pestañas:

- **Datos:** todos los eventos en forma de tabla (evento, fecha, lugar / descripción), además de la edad al fallecer.
- **Biografía:** la línea de vida con el matrimonio, los nacimientos de los hijos y la edad en cada evento. También lista los eventos en los que la persona fue padrino, madrina o testigo («Madrina en: Bautismo de …»); un clic lleva al niño o a la pareja.
- **Padrinos y testigos:** bajo el bautismo y el matrimonio aparece una línea «Padrinos:» o «Testigos:», en la biografía, en el área de detalle de la tabla de datos, en la vista de familia y en las tarjetas. Los padrinos con registro propio aparecen subrayados y se pueden pulsar; los que no lo tienen (de una nota «Paten: …» o del campo GEDCOM-L _GODP) aparecen como texto. ⓘ despliega una nota sobre el padrino, el icono de fuente abre la fuente. Los padrinos vivos que usted no puede ver aparecen solo como «Privado». Bajo la tabla de datos, la sección **Padrinazgos y testimonios** lista todos los bautismos y matrimonios en los que la persona fue padrino o testigo; un clic en el título la pliega. Ambas cosas requieren api4webtrees 1.11 o posterior; con un módulo más antiguo, la nota de padrinos sigue siendo simplemente una nota.
- **Tipo de matrimonio:** el matrimonio civil y el religioso aparecen como eventos separados con su tipo; las listas y la comprobación usan el civil cuando existen ambos.
- **Padres/hermanos**: padres y hermanos, incluidos los medios hermanos. Un clic cambia a esa persona.
- **Parejas/hijos**: las parejas a la izquierda, los hijos de la seleccionada a la derecha y, debajo, sus eventos (matrimonio, divorcio, residencia …) para añadir, editar y eliminar. Doble clic en una pareja o un hijo muestra su ficha; **+** añade una pareja o un hijo de esta pareja.
- **Nombre**: los nombres de pila, el apellido y el sufijo del nombre tienen campos propios al editar.
- **Sencillo / Completo** (al pie de la ficha): «Sencillo» muestra en la pestaña Datos un formulario con nombre, nacimiento, bautismo, religión, profesión, matrimonio por pareja, defunción y entierro para escribir directamente; «Completo» muestra la tabla de todos los eventos con la edad y marcas para nota y fuente (clic en el encabezado de una columna para ordenar).
  Solo se guarda lo que se ha cambiado; las fuentes, notas y demás detalles del evento se conservan. Si un evento existe más de una vez, el formulario edita el primero. Una fecha que el programa no puede interpretar («primavera de 1850») se guarda como texto de fecha. Se guarda al salir de un campo (Tab o clic en otro sitio), como en todo el programa. Una fecha no válida queda en rojo y no se envía hasta que se corrija o se elija «Guardar como texto de todos modos»; solo entonces, al cerrar o salir, el programa pregunta antes.
- **Notas**, **Fuentes**, **Multimedia**.
- **Mapa:** los lugares de la vida como lista con enlaces a OpenStreetMap.

## Pasar de una persona a otra

**Page Up** y **Page Down** pasan a la persona anterior o siguiente de la lista, **Ctrl+Home** y **Ctrl+End** a la primera y a la última. **Esc** cierra la ventana.

## Edición

Para editar se necesitan derechos de edición en webtrees. Los cambios llegan a webtrees de inmediato; según la configuración del árbol, se aplican al instante o esperan la aprobación de un moderador.

- **Cambiar un evento:** doble clic en la fila (o Editar). La fecha y el lugar se seleccionan, no se escriben: exacta, hacia, antes de, después de, entre; día, mes, año. Mientras escribe, se proponen lugares del árbol.
- **Añadir evento**, también eventos familiares como el matrimonio.
- **Añadir pariente** (Ctrl+N): crear padres, pareja, hijo o hermano con nombre y primeros eventos. En el navegador también está en el menú del botón derecho.
- **Registrar padrinos y testigos:** seleccione el bautismo o el matrimonio en la tabla de datos y haga clic en **Editar padrinos …** o **Editar testigos …** en el área de detalle de abajo (los matrimonios también en la pestaña Parejas/hijos). La lista se ordena con las flechas; ✕ quita una entrada. **Persona del árbol …** busca por nombre y vincula a la persona; **Sin registro …** añade a alguien que no tiene entrada propia, tal como figura en el libro parroquial: «Friedrich Plate, labrador en Celle». El rol (padrino/madrina, testigo u otro, como «comadrona») y una nota sobre la persona están bajo la lista. Los padrinos vinculados se guardan como lo hace webtrees, las personas sin registro en los campos GEDCOM-L _GODP y _WITN; las fuentes de un padrino se conservan. Los padrinos registrados solo en la persona (exportaciones antiguas) se trasladan al bautismo con una marca. Requiere api4webtrees 1.12 o posterior.
- **Tipo de matrimonio:** en el diálogo de evento de un matrimonio, **Tipo** permite elegir entre sin especificar, civil, religioso, pareja de hecho registrada y unión libre. Una segunda ceremonia (como la religiosa después de la civil) se añade como otro evento «Matrimonio» de la pareja.
- **Eliminar:** un evento o la persona entera, tras confirmar.
- **Editar en webtrees:** todo lo que el programa no puede hacer por sí mismo (nombres, fuentes nuevas, vincular medios) se hace en la página de la persona en el navegador. Véase [webtrees en el navegador](hilfe:webtrees).

## Fotos

**Añadir foto** elige un archivo del PC y lo adjunta a la persona; la imagen se reduce al límite de subida del servidor. Un clic en una imagen abre el visor.

## Imprimir y compartir

- **Archivo › Imprimir ficha personal …** (Ctrl+P) y **Ficha personal como PDF …**
- **Persona › Copiar texto de la persona** (Ctrl+Shift+C) copia todos los datos como texto al portapapeles, p. ej. para un correo electrónico o un procesador de textos.
- Más salidas: [Listas](hilfe:listen) (ficha personal como lista), [Gráficos](hilfe:tafeln), [Libros](hilfe:buecher).
