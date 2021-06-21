## Manual de usuario

#Nociones básicas
Al iniciar la aplicación, se muestra la pantalla principal. En ella se muestran 3 botones correspondientes a las 3 principales funcionalidades de la aplicación.
Al utilizar cada una de ellas, se pedirán algunos permisos al usuario, como puede ser el acceso a la cámara o a los archivos guardados. Si los permisos se rechazan un número determinado de veces fijado por la versión de Android que se utilice, tendrán que ser concedidos a través del gestor de permisos del sistema. Además, al rechazar los permisos, la aplicación volverá a ésta pantalla principal automáticamente.
 

![Main_activity](readme_images/main_menu.jpg)



#Conexión a la red escaneando un código QR
Pulsando sobre este botón, se abrirá una pantalla y directamente se piden los permisos de acceso a la cámara. 
 
Una vez se otorguen, se mostrará por la pantalla lo que se esté percibiendo con la cámara.
Al detectar un código QR, si no contiene información sobre una red, se mostrará un mensaje indicándolo. Si el código era correcto, el dispositivo vibrará y se comprobará que el Wifi del dispositivo está encendido, de forma que si está apagado se podrá encender mediante un diálogo dependiente de la versión de Android del dispositivo.
  
Al leerlo, dependiendo de la versión, se conectará directamente a la red en dispositivos Android 9 o inferior. En Android 10 o superior, aparecerá una notificación en la barra de notificaciones que permite guardar la información de la red, yes el dispositivo el que elige a qué red conectarse automáticamente.
 
Por último, debido a que la conexión a la red puede no ser instantánea como se ha explicado con las versiones más modernas de Android, se podrá pulsar el botón determinado para conectarse manualmente, el cual lleva a la pantalla de configuración del Wifi del dispositivo.
 

![Print_file](readme_images/readQR.jpg)


#Impresión de archivos
Pulsando el botón de imprimir un archivo abre una pantalla con tres botones, uno para seleccionar un archivo, otro para compartir el archivo seleccionado, y otro para mandarlo a una cola de impresión. Si se pulsan los dos últimos botones sin haber elegido un archivo, aparecerá un diálogo indicando que ha de seleccionarse uno. Una vez seleccionado, se previsualiza, permitiendo hacer zoom sobre él.
 
Si se selecciona un archivo con un formato que la aplicación no soporta, se muestra un diálogo indicándolo y explicando cuales son los formatos posibles.
 
Cuando se tenga el archivo preparado y se pulse el botón de imprimir, se abrirá el servicio de impresión proporcionado por el sistema, el cuál detecta las impresoras que hay en la red y permite cambiar la configuración de la impresión.
 
Finalmente, mientras dure la impresión, el sistema muestra el progreso en la barra de notificaciones del dispositivo.
 
![Print_file](readme_images/print.jpg)


#Escaneo de archivos
Al pulsar sobre el botón de escaneo, se abrirá una nueva pantalla con un botón de buscar escáneres, y al encontraros se mostrarán en una lista. Es obligatorio que el escáner y el teléfono estén conectados a la misma red Wifi.
Al seleccionar un escáner, se abre un menú con tres opciones: Escanear documento, imagen o texto. Esto crea ajustes preestablecidos que favorecen el tipo de escaneo que quiere realizarse, de forma que escanear imagen favorece escanear una hoja con una foto, escanear texto favorece una hoja con solo texto, y escanear documento faciliza escanear hojas donde hay texto e imágenes combinados.


![Search_scanner](readme_images/searchScanner.jpg)


Una vez seleccionado el tipo de escaneo que quiere realizarse, se abre una pantalla con las características del escáner.
En la parte superior, se muestra el nombre del escáner, su estado, y el estado del alimentador si dispone de él. Debajo aparece la configuración que puede cambiarse, recibida del proprio escáner.
 
Una vez seleccionadas las opciones preferidas se ha de pulsar el botón de escanear. En este momento, la aplicación valida las opciones con el escáner, y si no son válidas, se muestra un mensaje de advertencia y se permite cambiarlas. Si son correctas, se muestra un diálogo de carga mientras se muestre el escaneo, mostrado también la causa de un error en caso de que alguno ocurra. Además, se puede detener el escaneo borrando todos los archivos temporales que se hayan podido crear en el proceso.


![Scanner_settings](readme_images/scannerSettings.jpg)


Cuando se hayan escaneado todas las hojas, se mostrará una pantalla previsualizando el resultado. En este momento los archivos son todavía temporales, por lo que se permiten descartarse, guardarse definitivamente o compartirse por email según se requiera. Tras guardarse o descartarse, si hay mas archivos que previsualizar, se mostrarán automáticamente. Si no, se muestra un diálogo indicando que no hay más y la aplicación vuelve a la pantalla principal.


![Scan_result](readme_images/scanResult.jpg)
