#!/bin/sh
#
# description: Installs Tsung
#

# Package installation
export http_proxy=http://10.200.1.44:8080
apt-get install git-core -y --force-yes
apt-get install erlang erlang-src
apt-get install gnuplot-nox libtemplate-perl libhtml-template-perl libhtml-template-expr-perl autoconf -y --force-yes

cd /opt
mkdir usi2011_jaxio
cd usi2011_jaxio/
mkdir tsung
cd tsung/

# Create the injecteur
mkdir injecteur
cd injecteur/
git config --global http.proxy $http_proxy
git clone http://git.code.sf.net/p/usi2011/git usi2011
cd ..

# Install Tsung
wget http://tsung.erlang-projects.org/dist/tsung-1.3.3.tar.gz
tar -xzvf tsung-1.3.3.tar.gz
rm -f tsung-1.3.3.tar.gz
cd tsung-1.3.3/
./configure
make
sudo make install

# Clean up
export http_proxy=