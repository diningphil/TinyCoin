.PHONY: all clean doc compile

PEERSIM_JARS=""
LIB_JARS=`find -L libs/ -name "*.jar" | tr [:space:] :`

compile:
	mkdir -p classes
	javac -sourcepath src -classpath $(LIB_JARS):$(PEERSIM_JARS) -d classes `find -L -name "*.java"`

run:
	java -cp $(LIB_JARS):$(PEERSIM_JARS):classes peersim.Simulator tinyCoin.cfg

all: compile run

clean: 
	rm -fr classes doc
