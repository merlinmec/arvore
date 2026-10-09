# Build: compila o jar com o Maven Wrapper. As dependências ficam numa camada própria,
# então mudar só o código não baixa tudo de novo.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Runtime: só o JRE e o jar, rodando sem root.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 arvore
COPY --from=build /app/target/*.jar app.jar
USER arvore
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
