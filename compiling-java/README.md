# Compiling java source with tools.build

The accompanying blog post can be found [here](https://andersmurphy.com/2021/12/12/clojure-compiling-java-source-with-tools-build.html).

## To compile the java source

```
clj -T:build jcompile
```

## To run the repl

```
clj -M:dev
```

## To build the uberjar

```
clj -T:build uber
```

## Run uberjar

```
java -jar target/compiling-java-0.1.1.jar
```
