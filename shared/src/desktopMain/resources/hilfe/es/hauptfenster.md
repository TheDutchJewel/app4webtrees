# La ventana principal

En **Ver › Disposición** se elige entre tres disposiciones. Todas muestran el mismo árbol, ordenado de otra manera.

## Navegador (predeterminado)

Arriba la barra de herramientas, debajo la **persona central** con sus parejas e hijos y, a la derecha, sus antepasados, generación por generación.

- **Un clic** en una persona la convierte en persona central.
- **Doble clic** abre la [ficha personal](hilfe:person).
- **Botón derecho del ratón**: mostrar como persona central, editar, añadir marcador, abrir en webtrees.
- Los **hermanos** aparecen en el cuadro de información arriba a la izquierda (los medios hermanos con ½); un clic los convierte en persona central.
- **Teclado:** flecha derecha al padre (con Shift a la madre), izquierda al hijo, arriba/abajo por los hermanos; Enter abre la ficha (véase [Atajos de teclado](hilfe:tasten)).
- Las **generaciones** (de 2 a 7) y el **zoom** (−, +, ajustar) están en el propio navegador; la elección se recuerda.
- Un progenitor que falta aparece como «Padre desconocido» o «Madre desconocida». Con derechos de edición, **Añadir pariente** crea a la persona que falta.

**Barra de herramientas:** Ir a, Editar, Añadir pariente, Marcadores, Atrás, Adelante, Historial, Persona de inicio, Lista, Gráfico, Imprimir, Inicio, Fotos, Comprobar, Lugares, Fuentes, Ayuda, Salir. Si el ancho no alcanza, muestra solo los iconos (el nombre aparece al pasar el ratón) y, al final, el resto pasa al menú **Más**. Los rótulos se desactivan en **Ver › Etiquetas de la barra de herramientas**.

## Árbol en el centro

Tres columnas: a la izquierda la **lista de personas** con un campo de búsqueda (Ctrl+F salta a él), en el centro el **árbol** como reloj de arena alrededor de la persona central (arrastrar, zoom con la rueda del ratón, desplegar ramas hacia arriba) y a la derecha el **panel de persona** con los datos de la persona seleccionada.

Al alejar el zoom, una tarjeta muestra menos en lugar de hacerse más pequeña: primero sin imagen ni años, luego solo el nombre de pila y, por último, un cuadro en el color del sexo. Clic derecho en la lista: como persona central, perfil, añadir marcador, abrir en webtrees.

## Vista de familia

Como «Árbol en el centro», pero en el centro aparece la **familia** de la persona central: arriba los padres de ambos miembros de la pareja, en el centro la pareja con su matrimonio y abajo los hijos con sus fechas y matrimonios. Si la persona se casó más de una vez, cada matrimonio tiene su propia **pestaña**.

- **Un clic** selecciona a una persona; el panel de persona de la derecha la muestra.
- **Doble clic** la convierte en persona central: así se recorren las familias hacia arriba (padres) y hacia abajo (hijos).
- **Botón derecho del ratón**: Mostrar como persona central, Añadir pariente, Abrir en webtrees.
- Un progenitor que falta aparece como «Padre desconocido» o «Madre desconocida»; con derechos de edición, **+ añadir** lo crea.
- Los **hermanos** de la persona central se muestran encima de la pareja. **Teclado:** flecha arriba al padre (con Shift a la madre), abajo al hijo, izquierda/derecha por los hermanos, Tab a la pestaña siguiente.

La [tabla de personas](hilfe:tabelle) (Ctrl+4) muestra a todos en forma de tabla.

## Fuentes

**Ver › Fuentes** (Ctrl+5) o el icono **Fuentes** abre el gestor de fuentes: a la izquierda todas las fuentes del árbol con búsqueda y el número de citas; a la derecha la fuente seleccionada con autor, publicación, repositorio y signatura, texto, notas, medios y **Citada por**: cada persona y familia con los hechos que llevan la cita. Un clic selecciona a la persona; un doble clic la convierte en persona central.

Con derechos de edición: **+ Nueva fuente** (título, autor, publicación, abreviatura, repositorio con signatura –también un repositorio nuevo–, texto, nota), **Editar**, **Añadir escaneo o archivo …** (adjunta el archivo a la fuente como documento) y **Eliminar** (con aviso si la fuente todavía está citada). **Quitar las no utilizadas …** lista todas las fuentes que nadie cita, para marcarlas y eliminarlas. En el diálogo de cita, **Nueva fuente …** crea una fuente directamente y **Fuente desde archivo …** la crea a partir de un escaneo: título a partir del nombre del archivo, el archivo adjunto y la fuente seleccionada de inmediato.

Los **documentos** se adjuntan en dos lugares, como en webtrees: a la **fuente** (el libro parroquial digitalizado) o a la **cita** concreta (el escaneo de exactamente este bautismo). En ambos lugares se puede **subir un archivo** o elegir un **medio existente** del árbol; «desvincular» o ✕ solo quita el vínculo; el objeto multimedia y el archivo se conservan.

En la ficha personal, el área de detalle bajo la tabla de eventos (pestaña **Fuentes**) muestra cada cita con página, calidad, fecha, cita textual, notas y medios; un clic en el título abre la fuente aquí. Una fuente sin registro propio («según Martha Meier») aparece en cursiva.

Con derechos de edición, los botones de abajo son **+ Citar fuente** (buscar una fuente en el gestor o introducirla como texto, además de página, calidad, fecha, cita textual y nota), **Editar**, **Quitar**, **▲ ▼** (orden) y **Copiar a …**: la misma cita para otros eventos de esta persona, para padres, parejas e hijos (como cita general en el registro) o para la pareja. Solo se cambia lo que se introduce; todo lo demás de la cita y del evento se conserva.

En las disposiciones **Árbol en el centro** y **Vista de familia**, el botón **Fuentes** está en la barra superior; en el panel de persona de la derecha (pestaña Eventos) cada cita se puede pulsar y abre la fuente.

Requiere api4webtrees con el nivel de API 18; con servidores más antiguos, el icono abre como antes la lista de fuentes de webtrees.


## Lugares

**Ver › Lugares** (Ctrl+6) o el icono **Lugares** abre el gestor de lugares (desde api4webtrees 1.13): a la izquierda todos los lugares tal como figuran en los eventos, con búsqueda, el número de eventos y ◉ para «coordenadas conocidas». A la derecha el lugar seleccionado en pestañas: **Personas** (cada persona y familia con sus eventos allí; un clic selecciona a la persona, un doble clic la convierte en persona central), **Datos** (niveles, lugar superior, lugares incluidos, registro de lugar e identificador GOV), **Notas**, **Fuentes**, **Medios** y **Coordenadas** con mapa. En webtrees la nota, el identificador GOV y las coordenadas de un lugar están en su registro de lugar (_LOC, GEDCOM-L); si no lo hay, wtWin toma las coordenadas de los datos geográficos de webtrees o de un evento.

La pestaña **Datos** es la página del lugar: arriba una imagen (su propia foto en el registro de lugar o, si no, una sugerencia de Wikimedia Commons con crédito), mosaicos de nacimientos, matrimonios, defunciones y otros eventos, la **jerarquía** de GOV (hoy y antes, con años) y **Consultar** con GOV, GenWiki, Wikipedia, Archion, Matricula, Archivportal-D y la Biblioteca Digital Alemana. Para ello, el nombre del lugar y el identificador GOV se envían a gov.genealogy.net y Wikimedia; las respuestas se guardan 7 días en este PC. Una imagen de Wikimedia solo se muestra si el resultado está a 30 km como máximo de las coordenadas del lugar.

**Mapa** (conmutador sobre la lista) muestra todos los lugares con coordenadas: tamaño y color según el número de eventos, lugares cercanos como grupo gris con su número; un clic amplía. Un clic en un lugar lo selecciona, **Mostrar** pasa a sus datos. La búsqueda también filtra el mapa; arriba a la izquierda se indica cuántos lugares aún no tienen coordenadas.

Con permiso de edición, **Editar** abre los datos del lugar: identificador GOV (con **Buscar en GOV**), nota y coordenadas. **Buscar coordenadas …** consulta OpenStreetMap con el nombre del lugar; un clic en un resultado toma latitud y longitud. Todo se guarda en el registro de lugar (_LOC), que wtWin crea si hace falta; si el nombre del lugar aparece varias veces en el árbol, los eventos de ese lugar reciben una referencia a él. Los administradores pueden escribir además las coordenadas en los datos geográficos de webtrees: solo esos los leen los mapas del navegador.

**Renombrar y fusionar:** el nombre del lugar arriba es un campo de entrada. Cámbielo y confirme con la marca (o Intro); antes, wtWin indica cuántos eventos se cambiarán y qué lugares incluidos se mueven con él («Kortau, Allenstein» → «Kortau, Olsztyn»). Si el nuevo nombre ya existe, ambos lugares se fusionan: se conservan las notas, fuentes y medios de los registros de lugar; si el identificador GOV o las coordenadas difieren, valen los del destino. Los eventos bloqueados o confidenciales que no puede cambiar conservan el nombre anterior. Sin aceptación automática, los cambios esperan a un moderador como siempre. Consejo: corrija las erratas, pero deje los nombres históricos (Allenstein/Olsztyn) y únalos mediante el identificador GOV.

Los nombres de lugar en el panel de la persona y en la hoja de la persona se pueden pulsar, y cada campo de lugar relleno tiene un icono de lugar: ambos abren el lugar directamente en el gestor de lugares.

Con permiso de edición, la nota se escribe directamente en la pestaña **Notas**, en la pestaña **Medios** se añaden fotos y documentos (subir un archivo, un medio existente o uno del archivo; **desvincular** solo quita el vínculo) y en la pestaña **Fuentes** se cita una fuente para el lugar. Todo va al registro de lugar; si aún no existe, wtWin lo crea. La primera foto del registro de lugar pasa a ser la imagen de la parte superior de la página del lugar.

## Secciones: Inicio, Árbol, Fotos

- **Inicio** (Ctrl+1): saludo, próximos aniversarios, cambios recientes en el árbol, la persona de inicio. Los moderadores ven aquí los cambios pendientes y los aceptan o rechazan.
- **Árbol** (Ctrl+2): navegador, vista de árbol o familia.
- **Fotos** (Ctrl+3): todas las imágenes del árbol; un clic abre el visor. Si el módulo Sammlungen (colecciones) está instalado en el servidor, su archivo también aparece aquí.

## Ver

- **Aspecto:** claro, oscuro o como el sistema.
- **Idioma:** como el sistema o fijado en alemán, inglés, francés, neerlandés o español. Se aplica de inmediato; las etiquetas del servidor (tipos de evento, lugares) llegan en el nuevo idioma con la siguiente recarga. La ayuda y la comprobación de plausibilidad también cambian.
- **Código de colores:** colorea los antepasados de la persona de inicio según las cuatro líneas de los abuelos y sus descendientes en un quinto color (según Mary Hill). Requiere una persona de inicio.
- **Mostrar hermanos y sus parejas**, **Mostrar primos**: para la vista de árbol.
- **Generaciones:** cuántas generaciones de antepasados carga el árbol.

## Atrás, adelante, historial

Cada cambio de persona central entra en el historial. **Alt+Left** retrocede, **Alt+Right** vuelve a avanzar; el icono **Historial** lista las últimas personas. **Alt+Home** salta a la persona de inicio que webtrees conoce para su cuenta.

## Barra de estado

Abajo: servidor, cuenta (o «Invitado») y versión del programa. **F5** recarga el árbol, p. ej. después de hacer cambios en el navegador.
