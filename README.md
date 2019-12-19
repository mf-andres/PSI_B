
# Descripción

Este proyecto fue realizado en la materia de Programación de Sistemas Inteligentes del grado de Ingeniería en Tecnologías de Telecomunicaciones.

El objetivo del proyecto fue la implementación de varios agentes inteligentes que, mediante distintas estrategias, fueran capaces de competir en un juego.

El juego se basa en ir seleccionando elementos de una matriz, generada aleatoriamente, compuesta por vectores bidimensionales cuyas coordenadas son números enteros de 0 a 9. Se juega por parejas, de manera que un agente compite contra el otro. Para ello, cada agente genera un número entre 0 y la longitud de la matriz, el par de números resultantes determinará el elemento de la matriz escogido. Entonces, cada agente recibirá una puntuación relativa a la coordenada del vector escogido correspondiente siendo el ganador aquél que obtenga la mayor puntuación. La matriz no es conocida por los agentes, debe ser explorada.

## Alumno:

Andrés Muñoz Fernández

## Compilación y ejecución:

Para compilar (desde el fichero raíz):  
~~~
javac -cp lib/jade.jar src/PSI19/*.class
~~~

Para ejecutar: 
~~~
java -cp lib/jade.jar;bin jade.Boot -gui -agents "MainAgent:PSI19.MainAgent;P1:PSI19.LAAgent;P2:PSI19.LAAgent;P3:PSI19.LAwDistAgent" 
~~~

## Código fuente:

### 1 GameMatrix.java

Objeto que abstrae la matriz de puntuaciones. Contiene una matriz tridimensional (S x S x 2) el tamaño S de dicha matriz y métodos para su creación, modificación y consulta.

La inicialización de la matriz se basa en la asignación de valores a las sucesivas esquinas que la conforman, esto es: se asigna el vector bidimensional a una posición de la fila actual y luego se asigna el vector alterno a la posición de la columna correspondiente. Tras esto, se salta a la siguiente fila y columna. Así hasta completar la matriz.

Para la modificación de la matriz se asigna un vector a una celda aleatoria y, si dicha celda no forma parte de la diagonal, se asigna el vector alterno a la celda correspondiente.


### 2 ScoreMatrix.java

Vector de enteros para el almacenaje de las puntuaciones de una liga


### 3 Gui.java

Implementación de la interfaz gráfica de usuario.

La interfaz gráfica se compone de una barra de menús y tres paneles.

La barra de menús cuenta con las siguientes entradas:

File: con la opción New Game para empezar un nuevo juego

Edit: con la opción Parameters para modificar de los parámetros de juego
Window: con la opción Verbose para habilitar o deshabilitar el panel de información textual

El panel oeste contiene:

Una etiqueta en la que se informa al usuario del estado del juego. En ella aparecerán indicados los jugadores que competirán en la siguiente ronda. También anunciará el final de la liga.
Un botón New para empezar un nuevo juego
Un botón Stop que detendrá la liga en curso. La detención no se hará efectiva hasta que se intente jugar la próxima ronda.
Un botón Continue que sirve para reanudar la liga. La liga se detendrá hasta que el jugador pulse  Continue al final de cada ronda, al final de la liga y tras la modificación de la matriz.
Una etiqueta en la que se informa al usuario de los parámetros de juego.

El panel central contiene:

Un subpanel superior con:
Un panel con barra de desplazamiento en el que se listan los agentes detectados. Junto a la entrada de cada agente se mostrará su puntuación acumulada en la liga actual.
Un botón Update players para buscar agentes dispuestos a jugar.
Un subpanel inferior con:
Una etiqueta a modo de subtítulo para presentar la matriz de puntuaciones
Una panel con barra de desplazamiento que contiene una tabla para presentar la matriz de puntuaciones

El panel este contiene:

Un panel con barra de desplazamiento que contiene una área de texto en la que aparecen mensajes de control

Para jugar un liga, el usuario debe seguir los siguientes pasos:

Ir a Edit -> Parameters e introducir los parámetros de juego de la forma en que se indica (en un solo string separado por comas).
Presionar el botón New para que empiece la liga. Se presentará información sobre la ronda que se va a jugar. La etiqueta superior del panel oeste presentará los jugadores que se enfrentarán, la etiqueta inferior del panel oeste mostrará los parámetros introducidos y también se mostrará la matriz de puntuaciones.
Presionar el botón Continue para jugar el próximo juego, o continuar el juego tras haber sido modificada la matriz. Así hasta el final de la liga.
En cualquier momento el usuario puede detener la liga pulsando el botón Stop. Dicha acción se hará efectiva cuando el usuario pulse el botón Continue.

La interfaz gráfica es instanciada por el agente principal y se comunica con él mediante varios métodos para:

Comunicarle al agente principal los parámetros escogidos por el usuario y actualizar la etiqueta que los muestra
Imprimir los mensajes de control provenientes del agente principal
Actualizar la lista de agentes detectados
Actualizar la matriz de puntuaciones
Actualizar la etiqueta que muestra el estado del juego
Desbloquear el botón NewGame. Este botón no se desbloquea hasta que el número de agentes conectados no sea igual o superior al requerido por los parámetros.


### 4 MainAgent.java

Implementación del agente principal que controla la liga. Para ello se comunica con la interfaz gráfica y los agentes a los que tiene registrados.

Cuenta con cuatro métodos que invoca la interfaz gráfica para iniciar un nuevo juego (lanzando el behaviour GameManager), pausar la liga y actualizar parámetros y jugadores.

El behaviour GameManager se encarga de ejecutar una liga. Para ello genera una matriz y se comunica con los agentes  mediante mensajes ACL solicitandoles jugadas y comunicando sus resultados.


### 5 FixedAgent.java:

Implementación del agente que siempre escoge la misma fila y columna. En concreto, escoge siempre el valor correspondiente a la primera fila y columna de la matriz.


### 6 RandomAgent.java 

Implementación del agente aleatorio.


### 7 LAAgent.java

Implementación de un agente inteligente que hace uso de reinforcement learning para obtener la jugada óptima.

Se considera que la matriz no varía desde que se inicializa o resetea de forma que el algoritmo sólo cuenta con un estado.
Las acciones equivalen a la fila o columna a escoger.

En cada ronda el agente escoge una acción y aprende en base al resultado obtenido. Cuando la acción escogida ha resultado en una mayor puntuación respecto a la inmediatamente anterior se refuerza la probabilidad de escoger dicha acción y se disminuye la de escoger el resto.

Para añadir cierta aleatoriedad se genera un umbral aleatorio y se va sumando las probabilidades de las acciones. Cuando se supera el umbral se escoge la acción cuya última probabilidad fue computada.


### 8 LAwDistAgent.java

Implementación de otro agente inteligente con reinforcement learning.

Este agente varía respecto al anterior en que recompensa las acciones que generan un mayor distancia respecto de la anterior, entendiendo por distancia la diferencia entre las puntuación recibida y la del rival.

Es interesante comprobar que el primer agente, al intentar maximizar su propia puntuación perderá ante un agente que intente ganar la partida pero obtendrá una mayor puntuación que dicho agente si el número de agentes que intentan maximizar su propia puntuación es superior.
