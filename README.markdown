Challenge USI 2011 - Equipe "Jaxio and friends"
===============================================
Présentation
------------
Le [Challenge USI 2011](https://sites.google.com/a/octo.com/challengeusi2011/) est une compétition
dont le but est d'avoir l'application distribuée la plus performante et la plus robuste.

Vous trouverez plus d'informations sur [le site du Challenge](https://sites.google.com/a/octo.com/challengeusi2011/).

Ce dépôt GitHub contient le code de l'équipe n°10, dite "Jaxio and friends", composée de:

* [Julien Dubois](https://twitter.com/juliendubois)
* [Bernard Pons](https://twitter.com/ponsbernard)
* [Florent Ramiere](https://twitter.com/framiere)
* [Nicolas Romanetti](https://twitter.com/nromanetti)

Installation
------------
1. Clonez ce dépôt GitHub:
    git clone https://github.com/jaxio/usi2011.git
2. Installez [Cassandra 0.7.4](http://cassandra.apache.org/) (le numéro de version est important)
3. Configurez Cassandra: vous voulez probablement reconfigurer les répertoires définis par défaut dans $CASSANDRA_HOME/conf/cassandra.yaml
et $CASSANDRA_HOME/conf/log4j-server.properties. Nous en fournissons des exemples dans notre projet, voir: src/main/vfabric/config/cassandra
4. Lancez Cassandra
    $CASSANDRA_HOME/bin/./cassandra
5. Dans le répertoire d'exécution de Cassandra, lancez le client Cassandra:
    $CASSANDRA_HOME/bin/./cassandra-cli -host localhost -port 9160
6. Dans le client Cassandra, copiez/collez le script src/main/cassandra/init.script
7. Maintenant Cassandra est configuré et lancé!
8. L'application se compile normalement avec Maven
9. Pour lancer l'application, exécutez la classe usi2011.Main
10. Vérifiez que l'application est bien lancée: [http://localhost:9090](http://localhost:9090)