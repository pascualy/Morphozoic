// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Organism.
 */

#ifndef __ORGANISM__
#define __ORGANISM__

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <errno.h>
#include <vector>
#ifdef THREADS
#include <pthread.h>
#endif
#include "parameters.hpp"
#include "cell.hpp"
#include "../util/random.hpp"
#include "../util/octree.hpp"

namespace morphozoic
{
class Organism
{
public:

   // Parameters.
   Parameters *parameters;

   // Cells.
   vector<Cell *> cells;

   // Cell ID factory.
   int cellIDfactory;

   // Vessel size.
   float vesselRadius;

   // Random numbers.
   RANDOM randomSeed;
   Random *randomizer;

   // Cell bodies.
   vector<OctObject *> bodies;
   Octree              *bodyTracker;

   // Constructor.
#ifdef THREADS
   Organism(float vesselRadius, RANDOM randomSeed, int numThreads);
#else
   Organism(float vesselRadius, RANDOM randomSeed);
#endif

   // Initialize.
   void init(int numCells = 0);

   // Destructor.
   ~Organism();

   // Clear.
   void clear();

   // Create cell.
   Cell *createCell(int id = (-1));

   // Add cell and return ID.
   int addCell(Cell *);

   // Remove cell.
   void removeCell(int id);

   // Get cell by ID.
   Cell *getCell(int id);

   // Update.
   void update();

   // Draw.
   void draw(bool name = false);

   // Load and save.
   void load(FILE *fp);
   void save(FILE *fp);
   void import(FILE *fp);

private:

   void update(int threadNum);

#ifdef THREADS
   pthread_barrier_t updateBarrier;
   pthread_mutex_t   updateMutex;
   pthread_t         *threads;
   int               numThreads;
   struct ThreadInfo
   {
      Organism *organism;
      int      threadNum;
   };
   static void *updateThread(void *threadInfo);

   vector<OctObject *> moveList;
   bool                terminate;
#endif
};
}
#endif
