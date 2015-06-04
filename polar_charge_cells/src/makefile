# Morphozoic makefile

MAKEFLAGS += s

all:
	@echo "Making util..."
	@(cd util; make)
	@echo "Making GUI..."
	@(cd gui; make)
	@echo "Making morphozoic..."
	@(cd morphozoic; make)
	@echo "done"

clean:
	@(cd util; make clean)
	@(cd gui; make clean)
	@(cd morphozoic; make clean)

