# UnespSisgradCrawler
Este projeto contém:
- Um script em Java que permite navegar pelo Sisgrad (o sistema de graduação da Unesp).
- Um script em Python que permite navegar pelo Sisgrad.
- WebApp que facilita a navegação pelo Sisgrad, tendo o código em Java rodando num servidor. A intenção era fazer um webapp 
que se conectasse direto à Unesp, mas a política da mesma origem dos navegadores impede isso.

# Como usar:
Basta criar um arquivo com nome account.txt e inserir o seguinte texto:

user=seu_usuario

password=sua_senha

E rodar tanto o código em python como em java. Ambos são independentes.

# Compilando e rodando o java:

javac -encoding UTF-8 -cp ".:jsoup-1.8.3.jar" com/lucaszanella/SimpleRequest/SimpleRequest.java com/lucaszanella/UnespSisgradCrawler/SisgradCrawler.java Main.java

java -cp ".:jsoup-1.8.3.jar" Main