Solucion desarrollada y compilada en Ubuntu 24.04

1. BASE DE DATOS (Postgress). La ejecución de los servicios crean la base de datos en el motor y las tablas necesarias.
   Se debe configurar el host, usuario y password en e archivo: ./delivery/src/main/resources/application.properties
3. Abrir VS Code y clonar repositorio: https://github.com/DINSUASTI/delivery-backend.git
5. Ejecución WebHook (ejecutar en terminal vs code):
    cd /home/dinsuasti/delivery 
      ./mvnw spring-boot:run
6. Ejecución simulación sistema externo (ejecutar en otra terminal vs code):
    cd /home/dinsuasti/delivery
      ./mvnw -f ext-call/pom.xml spring-boot:run
7. Swagger webhook: http://localhost:8080/swagger-ui/index.html
8. Swagger servicio que simula plataforma externa: http://localhost:8081/swagger-ui/index.html
