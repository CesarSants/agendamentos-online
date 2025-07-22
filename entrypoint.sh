#!/bin/bash

echo "Inicializando Payara Micro com variáveis de ambiente..."

# Executa o Payara Micro usando as variáveis de ambiente já injetadas pelo Railway
exec java -jar payara-micro-5.2022.5.jar --deploy target/agendamentos-online.war --port $PORT