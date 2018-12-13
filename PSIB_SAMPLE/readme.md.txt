Alumno:

Andr�s Mu�oz Fern�ndez

Compilaci�n y ejecuci�n:

Para compilar (desde el fichero ra�z):  javac -cp lib/jade.jar src/PSI19/*.class

Para ejecutar: java -cp lib/jade.jar;bin jade.Boot -gui -agents "MainAgent:PSI19.MainAgent;P1:PSI19.LAAgent;P2:PSI19.LAAgent;P3:PSI19.LAwDistAgent" 

C�digo fuente:

1 GameMatrix.java

Objeto que abstrae la matriz de puntuaciones. Contiene una matriz tridimensional (S x S x 2) el tama�o S de dicha matriz y m�todos para su creaci�n, modificaci�n y consulta.

La inicializaci�n de la matriz se basa en la asignaci�n de valores a las sucesivas esquinas que la conforman, esto es: se asigna el vector bidimensional a una posici�n de la fila actual y luego se asigna el vector alterno a la posici�n de la columna correspondiente. Tras esto, se salta a la siguiente fila y columna. As� hasta completar la matriz.

Para la modificaci�n de la matriz se asigna un vector a una celda aleatoria y, si dicha celda no forma parte de la diagonal, se asigna el vector alterno a la celda correspondiente.


2 ScoreMatrix.java

Vector de enteros para el almacenaje de las puntuaciones de una liga


3 Gui.java

Implementaci�n de la interfaz gr�fica de usuario.

La interfaz gr�fica se compone de una barra de men�s y tres paneles.

La barra de men�s cuenta con las siguientes entradas:

File: con la opci�n New Game para empezar un nuevo juego
Edit: con la opci�n Parameters para modificar de los par�metros de juego
Window: con la opci�n Verbose para habilitar o deshabilitar el panel de informaci�n textual

El panel oeste contiene:

Una etiqueta en la que se informa al usuario del estado del juego. En ella aparecer�n indicados los jugadores que competir�n en la siguiente ronda. Tambi�n anunciar� el final de la liga.
Un bot�n New para empezar un nuevo juego
Un bot�n Stop que detendr� la liga en curso. La detenci�n no se har� efectiva hasta que se intente jugar la pr�xima ronda.
Un bot�n Continue que sirve para reanudar la liga. La liga se detendr� hasta que el jugador pulse  Continue al final de cada ronda, al final de la liga y tras la modificaci�n de la matriz.
Una etiqueta en la que se informa al usuario de los par�metros de juego.

El panel central contiene:

Un subpanel superior con:
Un panel con barra de desplazamiento en el que se listan los agentes detectados. Junto a la entrada de cada agente se mostrar� su puntuaci�n acumulada en la liga actual.
Un bot�n Update players para buscar agentes dispuestos a jugar.
Un subpanel inferior con:
Una etiqueta a modo de subt�tulo para presentar la matriz de puntuaciones
Una panel con barra de desplazamiento que contiene una tabla para presentar la matriz de puntuaciones

El panel este contiene:

Un panel con barra de desplazamiento que contiene una �rea de texto en la que aparecen mensajes de control

Para jugar un liga, el usuario debe seguir los siguientes pasos:

Ir a Edit -> Parameters e introducir los par�metros de juego de la forma en que se indica (en un solo string separado por comas).
Presionar el bot�n New para que empiece la liga. Se presentar� informaci�n sobre la ronda que se va a jugar. La etiqueta superior del panel oeste presentar� los jugadores que se enfrentar�n, la etiqueta inferior del panel oeste mostrar� los par�metros introducidos y tambi�n se mostrar� la matriz de puntuaciones.
Presionar el bot�n Continue para jugar el pr�ximo juego, o continuar el juego tras haber sido modificada la matriz. As� hasta el final de la liga.
En cualquier momento el usuario puede detener la liga pulsando el bot�n Stop. Dicha acci�n se har� efectiva cuando el usuario pulse el bot�n Continue.

La interfaz gr�fica es instanciada por el agente principal y se comunica con �l mediante varios m�todos para:

Comunicarle al agente principal los par�metros escogidos por el usuario y actualizar la etiqueta que los muestra
Imprimir los mensajes de control provenientes del agente principal
Actualizar la lista de agentes detectados
Actualizar la matriz de puntuaciones
Actualizar la etiqueta que muestra el estado del juego
Desbloquear el bot�n NewGame. Este bot�n no se desbloquea hasta que el n�mero de agentes conectados no sea igual o superior al requerido por los par�metros.


4 MainAgent.java

Implementaci�n del agente principal que controla la liga. Para ello se comunica con la interfaz gr�fica y los agentes a los que tiene registrados.

Cuenta con cuatro m�todos que invoca la interfaz gr�fica para iniciar un nuevo juego (lanzando el behaviour GameManager), pausar la liga y actualizar par�metros y jugadores.

El behaviour GameManager se encarga de ejecutar una liga. Para ello genera una matriz y se comunica con los agentes  mediante mensajes ACL solicitandoles jugadas y comunicando sus resultados.


5 FixedAgent.java:

Implementaci�n del agente que siempre escoge la misma fila y columna. En concreto, escoge siempre el valor correspondiente a la primera fila y columna de la matriz.


6 RandomAgent.java 

Implementaci�n del agente aleatorio.


7 LAAgent.java

Implementaci�n de un agente inteligente que hace uso de reinforcement learning para obtener la jugada �ptima.

Se considera que la matriz no var�a desde que se inicializa o resetea de forma que el algoritmo s�lo cuenta con un estado.
Las acciones equivalen a la fila o columna a escoger.

En cada ronda el agente escoge una acci�n y aprende en base al resultado obtenido. Cuando la acci�n escogida ha resultado en una mayor puntuaci�n respecto a la inmediatamente anterior se refuerza la probabilidad de escoger dicha acci�n y se disminuye la de escoger el resto.

Para a�adir cierta aleatoriedad se genera un umbral aleatorio y se va sumando las probabilidades de las acciones. Cuando se supera el umbral se escoge la acci�n cuya �ltima probabilidad fue computada.


8 LAwDistAgent.java

Implementaci�n de otro agente inteligente con reinforcement learning.

Este agente var�a respecto al anterior en que recompensa las acciones que generan un mayor distancia respecto de la anterior, entendiendo por distancia la diferencia entre las puntuaci�n recibida y la del rival.

Es interesante comprobar que el primer agente, al intentar maximizar su propia puntuaci�n perder� ante un agente que intente ganar la partida pero obtendr� una mayor puntuaci�n que dicho agente si el n�mero de agentes que intentan maximizar su propia puntuaci�n es superior.


