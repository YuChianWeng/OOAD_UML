SRC_DIR    = src
TEST_DIR   = test
BIN_DIR    = bin
TEST_BIN   = test-bin
JAR_NAME   = uml-editor.jar
MAIN_CLASS = uml.app.Main

# Collect every .java file under src/ and test/
SOURCES = $(shell find $(SRC_DIR) -name "*.java")
TEST_SOURCES = $(shell find $(TEST_DIR) -name "*.java" 2>/dev/null)
TEST_CLASSES = ModelSmokeTest uml.ui.ToolbarTransientModeTest uml.ui.SelectModeMultiMoveTest uml.ui.ResizeAnchorClampTest

.PHONY: all compile test jar run clean

all: jar

compile: $(SOURCES)
	mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SOURCES)

test: compile
	mkdir -p $(TEST_BIN)
	javac -cp $(BIN_DIR) -d $(TEST_BIN) $(TEST_SOURCES)
	@for test_class in $(TEST_CLASSES); do \
		java -cp $(BIN_DIR):$(TEST_BIN) $$test_class; \
	done

jar: compile
	printf 'Main-Class: $(MAIN_CLASS)\n' > manifest.txt
	jar cfm $(JAR_NAME) manifest.txt -C $(BIN_DIR) .
	rm manifest.txt

run: jar
	java -jar $(JAR_NAME)

clean:
	rm -rf $(BIN_DIR) $(TEST_BIN) $(JAR_NAME)
