SRC_DIR    = src
BIN_DIR    = bin
JAR_NAME   = uml-editor.jar
MAIN_CLASS = uml.app.Main

# Collect every .java file under src/
SOURCES = $(shell find $(SRC_DIR) -name "*.java")

.PHONY: all compile jar run clean

all: jar

compile: $(SOURCES)
	mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SOURCES)

jar: compile
	printf 'Main-Class: $(MAIN_CLASS)\n' > manifest.txt
	jar cfm $(JAR_NAME) manifest.txt -C $(BIN_DIR) .
	rm manifest.txt

run: jar
	java -jar $(JAR_NAME)

clean:
	rm -rf $(BIN_DIR) $(JAR_NAME)
