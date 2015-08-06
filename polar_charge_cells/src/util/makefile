# Utility makefile

UTIL_SOURCES = baseObject.cpp camera.cpp fileio.cpp frameRate.cpp frustum.cpp \
             gettime.cpp log.cpp md5.cpp octree.cpp quaternion.cpp random.cpp spacial.cpp

UTIL_OBJECTS = $(UTIL_SOURCES:%.cpp=%.o)

UTIL_LIB = ../../lib/libutil.a

CC = gcc

# Windows code is always position independent
SHELL = /bin/sh
OSNAME := $(shell uname -o)
ifeq ($(OSNAME),Cygwin)
PICFLAG =
else
PICFLAG = -fPIC
endif

CCFLAGS = -DUNIX $(PICFLAG) -O3

all : $(UTIL_LIB)

$(UTIL_LIB) : $(UTIL_OBJECTS)
	ar -cr -o $(UTIL_LIB) $(UTIL_OBJECTS)

baseObject.o: baseObject.hpp baseObject.cpp spacial.hpp
	$(CC) $(CCFLAGS) -c baseObject.cpp

camera.o: camera.hpp camera.cpp baseObject.hpp spacial.hpp
	$(CC) $(CCFLAGS) -c camera.cpp

fileio.o: fileio.h fileio.cpp
	$(CC) $(CCFLAGS) -c fileio.cpp

frameRate.o: frameRate.hpp frameRate.cpp
	$(CC) $(CCFLAGS) -c frameRate.cpp

frustum.o: frustum.hpp frustum.cpp
	$(CC) $(CCFLAGS) -c frustum.cpp

gettime.o: gettime.h gettime.cpp
	$(CC) $(CCFLAGS) -c gettime.cpp
	
log.o: log.cpp log.hpp
	$(CC) $(CCFLAGS) -c log.cpp

md5.o: md5.cpp md5.h
	$(CC) $(CCFLAGS) -c md5.cpp

octree.o: octree.hpp octree.cpp
	$(CC) $(CCFLAGS) -c octree.cpp

quaternion.o: quaternion.cpp quaternion.hpp
	$(CC) $(CCFLAGS) -c quaternion.cpp

random.o: random.hpp random.cpp
	$(CC) $(CCFLAGS) -c random.cpp

spacial.o: spacial.hpp spacial.cpp
	$(CC) $(CCFLAGS) -c spacial.cpp

clean:
	/bin/rm -f *.o
