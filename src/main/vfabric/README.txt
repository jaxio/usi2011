Installation et utilisation du cloud vfabric.

PREREQUIS
---------
Sur votre machine locale:

Il faut que le server vfabric soit défini dans votre /etc/hosts

echo "92.103.37.14	vfabric" >> /etc/hosts

Il faut également avoir déposé sa clef publique SSH sur le serveur vfabric:

ssh root@vfabric "echo `cat ~/.ssh/id_rsa.pub` >> ~/.ssh/authorized_keys"

On se connecte toujours en ROOT pour plus de simplicité (lancement de services, mises à jour, disponibilité du port 80...)

Note temporaire sur tsung:
Les scripts tsung sont sur usi1
  ssh usi1
  cd ~/.tsung/jaxio/usi2011/


UTILISATION
-----------
Dans le répertoire "local" sont les scripts à utiliser depuis son PC/Mac:

deploy.sh         --> met à jour le jeu sur le cloud depuis la machine locale
tsung_run.sh      --> lance Tsung sur le cloud depuis la machine locale

Dans le répertoire "cloud" sont les scripts à utiliser sur le serveur frontal du cloud (192.168.1.1)

Utilisation courante:
game_service.sh           --> Service Unix pour démarrer/arrêter le serveur de jeu ( start stop restart )
cassandra_service.sh      --> Service Unix pour démarrer/arrêter le serveur Cassandra ( start stop restart )

Installation:
vfabric_tuning.sh         --> nettoye et optimise le serveur, à faire à l'installation
game_install.sh           --> initialise les répertoires, installe cassandra et maven

Mise à jour (utilisé par le script deploy.sh en local):
game_update.sh            --> met à jour le jeu sur un noeud (copie les fichiers de conf, rebuild)
game_update_all_nodes.sh  --> met à jour le jeu sur tous les noeuds

Tests Tsung:
tsung_install.sh          --> installe Tsung (à ne faire que sur l'injecteur, ça prend de la place)
tsung_run.sh              --> lance Tsung

REPERTOIRES SUR LE CLOUD
------------------------
Tout est installé dans /opt/usi2011_jaxio

apache-cassandra-0.7.4 --> Cassandra
apache-maven-3.0.3     --> Maven
data                   --> Données de Cassandra
game                   --> Le jeu: projet copié sur le serveur par le script deploy.sh
log                    --> Logs
tsung                  --> Installation de Tsung (uniquement si le serveur sert aussi d'injecteur)


INSTALLATION D'UN NOUVEAU NOEUD
-------------------------------
Copier vfabric_tuning.sh sur le noeud:
scp /opt/usi2011_jaxio/game/src/main/vfabric/cloud/vfabric_tuning.sh user@vfabricXXX:tmp/

Se connecter sur le noeud:
ssh user@vfabricXXX
sudo ./vfabric_tuning.sh

Puis refaire un "deploy.sh" en local pour regénérer tous les noeuds (nécessaire pour regénérer
correctement le initial_token de Cassandra).

PERFORMANCES
------------

# voir les débits et nombre de connections TCP
tcptrack -i eth0

# versatile tool for generating system resource statistics
dstat

# top
htop 