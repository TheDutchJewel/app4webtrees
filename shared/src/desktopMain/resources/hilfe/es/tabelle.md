# Tabla de personas

La **tabla de personas** muestra a todas las personas del árbol genealógico en columnas: ID, nombre, sexo, nacimiento, lugar de nacimiento, defunción, lugar de defunción y profesión. Se abre con **Ver › Tabla de personas** (Ctrl+4) o con el icono **Lista**. La ventana puede quedarse abierta junto a la ventana principal.

## Ordenar

Haga clic en el encabezado de una columna para ordenar por ella; un segundo clic invierte el orden. Las fechas se ordenan por el calendario, no por su texto; las personas sin valor siempre quedan al final.

## Filtrar

Debajo de cada encabezado de columna hay un campo de filtro. Varios filtros se aplican a la vez.

- **Texto:** encuentra fragmentos, sin distinguir mayúsculas y minúsculas: «hann» encuentra Hannover.
- **1800-1850** en nacimiento o defunción: años desde–hasta; también **-1850** o **1800-**.
- `!`: el campo está vacío. Así se encuentran lagunas, por ejemplo todas las personas sin lugar de nacimiento.
- `*`: el campo está relleno.

**Borrar filtros** restablece todos los campos. La línea superior indica cuántas personas coinciden.

Si falta el nacimiento, se muestra el bautismo (con ~); si falta la defunción, el entierro (con □). Las personas privadas no se listan.

## Trabajar con la tabla

- **Clic:** selecciona a la persona; el panel de persona de la ventana principal la muestra.
- **Doble clic** o **Enter:** mostrar a la persona como persona central.
- **Teclas de flecha, Page Up/Page Down:** desplazarse por la tabla.
- **Clic derecho:** Mostrar como persona central, Editar persona, Abrir en webtrees.
- **Guardar como CSV …** escribe las filas mostradas en ese momento en un archivo para programas de hojas de cálculo.

La tabla necesita el árbol completo de una vez; el servidor debe tener api4webtrees 1.9 o posterior (véase [Listas](hilfe:listen)).
