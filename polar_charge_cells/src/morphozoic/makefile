# Morphozoic makefile.

MORPHOZOIC_SOURCES = morphozoic.cpp organism.cpp cell.cpp parameters.cpp

MORPHOZOIC_OBJECTS = $(MORPHOZOIC_SOURCES:%.cpp=%.o)

MORPHOZOIC = ../../bin/morphozoic

CC = gcc

CCFLAGS = -DUNIX -DTHREADS -I../../include -O3

LINKLIBS = -L../../lib -lutil -lsxmlgui -lglpng -lglut -lGLU -lGL -lm -lpthread -lstdc++

all: $(MORPHOZOIC)

$(MORPHOZOIC): $(MORPHOZOIC_OBJECTS)
	$(CC) -o $(MORPHOZOIC) $(MORPHOZOIC_OBJECTS) $(LINKLIBS)

morphozoic.o: morphozoic.h morphozoic.cpp parameters.hpp
	$(CC) -DMORPHOZOIC_MAIN $(CCFLAGS) -c morphozoic.cpp

organism.o: organism.hpp organism.cpp parameters.hpp
	$(CC) $(CCFLAGS) -c organism.cpp

cell.o: cell.hpp cell.cpp parameters.hpp
	$(CC) $(CCFLAGS) -c cell.cpp

parameters.o: parameters.hpp parameters.cpp
	$(CC) $(CCFLAGS) -c parameters.cpp

clean:
	/bin/rm -f *.o





