// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Organism.
 */

#include "organism.hpp"
#include "../util/fileio.h"
using namespace morphozoic;

// Constructor.
#ifdef THREADS
Organism::Organism(float vesselRadius, RANDOM randomSeed, int numThreads)
#else
Organism::Organism(float vesselRadius, RANDOM randomSeed)
#endif
{
   parameters = new Parameters();
   assert(parameters != NULL);
   this->vesselRadius = vesselRadius;
   this->randomSeed   = randomSeed;
   randomizer         = NULL;
   bodyTracker        = NULL;

#ifdef THREADS
   // Start additional update threads.
   assert(numThreads > 0);
   terminate        = false;
   this->numThreads = numThreads;
   if (numThreads > 1)
   {
      if (pthread_barrier_init(&updateBarrier, NULL, numThreads) != 0)
      {
         fprintf(stderr, "pthread_barrier_init failed, errno=%d\n", errno);
         exit(1);
      }
      if (pthread_mutex_init(&updateMutex, NULL) != 0)
      {
         fprintf(stderr, "pthread_mutex_init failed, errno=%d\n", errno);
         exit(1);
      }
      threads = new pthread_t[numThreads - 1];
      assert(threads != NULL);
      struct ThreadInfo *info;
      for (int i = 0; i < numThreads - 1; i++)
      {
         info = new struct ThreadInfo;
         assert(info != NULL);
         info->organism  = this;
         info->threadNum = i + 1;
         if (pthread_create(&threads[i], NULL, updateThread, (void *)info) != 0)
         {
            fprintf(stderr, "pthread_create failed, errno=%d\n", errno);
            exit(1);
         }
      }
   }
#endif
}


// Initialize organism.
void Organism::init(int numCells)
{
   clear();
   randomizer = new Random(randomSeed);
   assert(randomizer != NULL);
   bodyTracker = new Octree(0.0f, 0.0f, 0.0f, vesselRadius * 1.5f, parameters->MIN_RADIUS);
   assert(bodyTracker != NULL);
   for (int i = 0; i < numCells; i++)
   {
      createCell();
   }
}


// Destructor.
Organism::~Organism()
{
#ifdef THREADS
   // Terminate threads.
   if (numThreads > 1)
   {
      // Unblock threads waiting on update barrier.
      terminate = true;
      update();
      for (int i = 0; i < numThreads - 1; i++)
      {
         pthread_join(threads[i], NULL);
         pthread_detach(threads[i]);
      }
      pthread_mutex_destroy(&updateMutex);
      pthread_barrier_destroy(&updateBarrier);
      delete threads;
   }
#endif
   clear();
   if (parameters != NULL)
   {
      delete parameters;
   }
}


// Clear.
void Organism::clear()
{
   int i, j;

   cellIDfactory = 0;
   bodies.clear();
   if (bodyTracker != NULL)
   {
      delete bodyTracker;
   }
   bodyTracker = NULL;
   for (i = 0, j = (int)cells.size(); i < j; i++)
   {
      if (cells[i] != NULL)
      {
         delete cells[i];
      }
   }
   cells.clear();
   if (randomizer != NULL)
   {
      delete randomizer;
   }
   randomizer = NULL;
}


// Create cell and add to organism.
Cell *Organism::createCell(int id)
{
   int   i, j;
   Cell  *cell;
   float radius;

   vector<float> charges[6];
   Vector        position;
   Vector        velocity;
   OctObject     *b;

   if (bodyTracker == NULL)
   {
      init(0);
   }
   radius = (float)randomizer->RAND_INTERVAL(parameters->MIN_RADIUS, parameters->MAX_RADIUS);
   for (i = 0; i < 6; i++)
   {
      charges[i].resize(parameters->CHARGES);
      for (j = 0; j < parameters->CHARGES; j++)
      {
         charges[i][j] = randomizer->RAND_INTERVAL(-parameters->MAX_CHARGE, parameters->MAX_CHARGE);
      }
   }
   position.x = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   position.y = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   position.z = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   position.Normalize((float)randomizer->RAND_INTERVAL(0.0f, vesselRadius - radius));
   if (id == -1)
   {
      cell = new Cell(cellIDfactory, parameters, randomizer,
                      radius, charges, position, velocity);
      cellIDfactory++;
   }
   else
   {
      cell = new Cell(id, parameters, randomizer,
                      radius, charges, position, velocity);
   }
   assert(cell != NULL);
   cells.push_back(cell);
   cell->forces.x = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   cell->forces.y = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   cell->forces.z = (float)randomizer->RAND_INTERVAL(-1.0f, 1.0f);
   cell->forces.Normalize((float)randomizer->RAND_INTERVAL(0.0f, parameters->MAX_FORCE));
   b = new OctObject(cell->position, (void *)cell);
   assert(b != NULL);
   bodies.push_back(b);
   bodyTracker->insert(b);
   return(cell);
}


// Add cell to organism.
int Organism::addCell(Cell *cell)
{
   OctObject *b;

   if (bodyTracker == NULL)
   {
      init(0);
   }
   cell->setID(cellIDfactory);
   cellIDfactory++;
   cell->setParameters(parameters);
   cells.push_back(cell);
   b = new OctObject(cell->position, (void *)cell);
   assert(b != NULL);
   bodies.push_back(b);
   bodyTracker->insert(b);
   return(cell->getID());
}


// Remove cell from system.
void Organism::removeCell(int id)
{
   int  i, j;
   Cell *cell;

   vector<Cell *>      tmpCells;
   vector<OctObject *> tmpBodies;

   if (bodyTracker == NULL)
   {
      init(0);
   }
   for (i = 0, j = (int)bodies.size(); i < j; i++)
   {
      cell = (Cell *)bodies[i]->client;
      if (cell->id == id)
      {
         bodyTracker->remove(bodies[i]);
      }
      else
      {
         tmpBodies.push_back(bodies[i]);
      }
   }
   bodies.clear();
   for (i = 0, j = (int)tmpBodies.size(); i < j; i++)
   {
      bodies.push_back(tmpBodies[i]);
   }
   for (i = 0, j = (int)cells.size(); i < j; i++)
   {
      if (cells[i]->getID() != id)
      {
         tmpCells.push_back(cells[i]);
      }
      else
      {
         delete cells[i];
      }
   }
   cells.clear();
   for (i = 0, j = (int)tmpCells.size(); i < j; i++)
   {
      cells.push_back(tmpCells[i]);
   }
}


// Get cell by ID.
Cell *Organism::getCell(int id)
{
   int i, j;

   if (bodyTracker == NULL)
   {
      init(0);
   }
   for (i = 0, j = (int)cells.size(); i < j; i++)
   {
      if (cells[i]->getID() == id)
      {
         return(cells[i]);
      }
   }
   return(NULL);
}


// Update.
void Organism::update()
{
   // Update with base thread.
   update(0);
}


void Organism::update(int threadNum)
{
   int    i, j, k, q, r, s;
   float  d;
   Vector x, f, n, p, v, p1, p2;
   Cell   *c1, *c2;

   list<OctObject *>           searchList;
   list<OctObject *>::iterator searchItr;

   // Ensure initialization.
   if ((threadNum == 0) && (bodyTracker == NULL))
   {
      init(0);
   }

#ifdef THREADS
   // Synchronize threads.
   if (numThreads > 1)
   {
      i = pthread_barrier_wait(&updateBarrier);
      if ((i != PTHREAD_BARRIER_SERIAL_THREAD) && (i != 0))
      {
         fprintf(stderr, "pthread_barrier_wait failed, errno=%d\n", errno);
         exit(1);
      }
      if (terminate)
      {
         if (threadNum == 0)
         {
            return;
         }
         pthread_exit(NULL);
      }
   }
#endif

   // Do charge and overlap forces.
#ifdef THREADS
   if (numThreads > 1)
   {
      pthread_barrier_wait(&updateBarrier);
   }
#endif
   for (i = 0, j = (int)bodies.size(); i < j; i++)
   {
#ifdef THREADS
      if ((i % numThreads) != threadNum)
      {
         continue;
      }
#endif
      c1 = (Cell *)bodies[i]->client;
      searchList.clear();
      bodyTracker->search(c1->position, parameters->MAX_INTERACTION_RANGE, searchList);
      for (searchItr = searchList.begin();
           searchItr != searchList.end(); searchItr++)
      {
         c2 = (Cell *)(*searchItr)->client;
         if (c1 == c2)
         {
            continue;
         }

         // Prevent overlapping bodies.
         x = c2->position - c1->position;
         d = x.Magnitude();
         if (d < tol)
         {
            switch (randomizer->RAND_CHOICE(3))
            {
            case 0:
               if (randomizer->RAND_BOOL())
               {
                  c1->position.x += tol;
                  c2->position.x -= tol;
               }
               else
               {
                  c1->position.x -= tol;
                  c2->position.x += tol;
               }
               break;

            case 1:
               if (randomizer->RAND_BOOL())
               {
                  c1->position.y += tol;
                  c2->position.y -= tol;
               }
               else
               {
                  c1->position.y -= tol;
                  c2->position.y += tol;
               }
               break;

            case 2:
               if (randomizer->RAND_BOOL())
               {
                  c1->position.z += tol;
                  c2->position.z -= tol;
               }
               else
               {
                  c1->position.z -= tol;
                  c2->position.z += tol;
               }
               break;
            }
         }
         x.Normalize();
         if (d <= (c1->radius + c2->radius))
         {
            f = x * parameters->OVERLAP_REPULSION_FORCE;
#ifdef THREADS
            if (numThreads > 1)
            {
               pthread_mutex_lock(&updateMutex);
            }
#endif
            c1->forces -= f;
            c2->forces += f;
#ifdef THREADS
            if (numThreads > 1)
            {
               pthread_mutex_unlock(&updateMutex);
            }
#endif
            continue;
         }

         // Accumulate oriented charge forces: gaussians with max=charge product.
         f.Zero();
         for (k = 0; k < 6; k++)
         {
            switch (k)
            {
            case Cell::UP:
               v  = c1->orientationToVector(Cell::UP);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;

            case Cell::DOWN:
               v  = c1->orientationToVector(Cell::DOWN);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;

            case Cell::RIGHT:
               v  = c1->orientationToVector(Cell::RIGHT);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;

            case Cell::LEFT:
               v  = c1->orientationToVector(Cell::LEFT);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;

            case Cell::BACK:
               v  = c1->orientationToVector(Cell::BACK);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;

            case Cell::FRONT:
               v  = c1->orientationToVector(Cell::FRONT);
               p1 = c1->position + (v * c1->radius);
               v  = -v;
               q  = (int)c2->vectorToOrientation(v);
               p2 = c2->position + (c2->orientationToVector((Cell::ORIENTATION)q) * c2->radius);
               break;
            }
            x = p2 - p1;
            d = x.Magnitude();
            for (r = 0, s = (int)c1->charges[k].size(); r < s; r++)
            {
               f += x * (c1->charges[k][r] * c2->charges[q][r]) *
                    (float)exp(-(double)((d * d) /
                                         (parameters->CHARGE_GAUSSIAN_SPREAD *
                                          parameters->CHARGE_GAUSSIAN_SPREAD)));
            }
         }
#ifdef THREADS
         if (numThreads > 1)
         {
            pthread_mutex_lock(&updateMutex);
         }
#endif
         c1->forces -= f;
         c2->forces += f;
#ifdef THREADS
         if (numThreads > 1)
         {
            pthread_mutex_unlock(&updateMutex);
         }
#endif
      }
   }

   // Update velocities and positions.
   for (i = 0, j = (int)cells.size(); i < j; i++)
   {
#ifdef THREADS
      if ((i % numThreads) != threadNum)
      {
         continue;
      }
#endif
      cells[i]->update(parameters->UPDATE_STEP);
   }

   // Contain bodies inside vessel.
   for (i = 0, j = (int)bodies.size(); i < j; i++)
   {
#ifdef THREADS
      if ((i % numThreads) != threadNum)
      {
         continue;
      }
#endif
      c1 = (Cell *)bodies[i]->client;
      d  = c1->position.Magnitude();
      if (d > vesselRadius * 1.25f)
      {
         c1->position.Normalize(vesselRadius - c1->radius);
      }
      p = c1->position + (c1->velocity * parameters->UPDATE_STEP);
      if ((d >= (vesselRadius - c1->radius)) && (d < p.Magnitude()))
      {
         if (d > tol)
         {
            n = -c1->position;
            n.Normalize();
            v            = c1->velocity;
            c1->velocity = v - (2.0f * (v * n) * n);
         }
         continue;
      }
   }

#ifdef THREADS
   // Update body tracker.
   if (numThreads > 1)
   {
      if (threadNum == 0)
      {
         moveList.clear();
      }
      pthread_barrier_wait(&updateBarrier);
   }
#endif
   for (i = 0, j = (int)bodies.size(); i < j; i++)
   {
#ifdef THREADS
      if ((i % numThreads) != threadNum)
      {
         continue;
      }
#endif
      c1 = (Cell *)bodies[i]->client;
#ifdef THREADS
      if (numThreads > 1)
      {
         bodies[i]->position = c1->position;

         // Save object if move causes a tracker change.
         if (!bodies[i]->isInside(bodies[i]->node))
         {
            pthread_mutex_lock(&updateMutex);
            moveList.push_back(bodies[i]);
            pthread_mutex_unlock(&updateMutex);
         }
      }
      else
      {
         bodies[i]->move(c1->position);
      }
#else
      bodies[i]->move(b1->position);
#endif
   }
#ifdef THREADS
   // Update tracker with moved objects.
   if (numThreads > 1)
   {
      pthread_barrier_wait(&updateBarrier);
      if (threadNum == 0)
      {
         for (i = 0, j = (int)moveList.size(); i < j; i++)
         {
            c1 = (Cell *)moveList[i]->client;
            moveList[i]->move(c1->position);
         }
      }
   }
#endif

#ifdef THREADS
   // Re-group threads.
   if (numThreads > 1)
   {
      pthread_barrier_wait(&updateBarrier);
   }
#endif
}


#ifdef THREADS
// Update thread.
void *Organism::updateThread(void *arg)
{
   struct ThreadInfo *info     = (struct ThreadInfo *)arg;
   Organism          *organism = info->organism;
   int               threadNum = info->threadNum;

   delete info;
   while (true)
   {
      organism->update(threadNum);
   }
   return(NULL);
}


#endif

// Draw.
void Organism::draw(bool name)
{
   for (int i = 0, j = (int)cells.size(); i < j; i++)
   {
      cells[i]->draw(name);
   }
}


// Load organism.
void Organism::load(FILE *fp)
{
   int       i, j;
   OctObject *b;
   Cell      *cell;

   clear();
   init(0);
   parameters->load(fp);
   FREAD_INT(&cellIDfactory, fp);
   FREAD_FLOAT(&vesselRadius, fp);
   randomizer->RAND_LOAD(fp);
   FREAD_INT(&j, fp);
   for (i = 0; i < j; i++)
   {
      cell = new Cell();
      assert(cell != NULL);
      cell->setParameters(parameters);
      cell->setRandomizer(randomizer);
      cell->load(fp);
      cells.push_back(cell);
      b = new OctObject(cell->position, (void *)cell);
      assert(b != NULL);
      bodies.push_back(b);
      bodyTracker->insert(b);
   }
}


// Save organism.
void Organism::save(FILE *fp)
{
   int  i, j;
   Cell *cell;

   if (bodyTracker == NULL)
   {
      init(0);
   }
   parameters->save(fp);
   FWRITE_INT(&cellIDfactory, fp);
   FWRITE_FLOAT(&vesselRadius, fp);
   randomizer->RAND_SAVE(fp);
   j = (int)cells.size();
   FWRITE_INT(&j, fp);
   for (i = 0; i < j; i++)
   {
      cell = cells[i];
      cell->save(fp);
   }
}


// Import organism into current one.
void Organism::import(FILE *fp)
{
   int      i, j;
   Organism *organism;
   Cell     *cell;

#ifdef THREADS
   organism = new Organism(vesselRadius, randomSeed, 1);
#else
   organism = new Organism(vesselRadius, randomSeed);
#endif
   assert(organism != NULL);
   organism->load(fp);

   for (i = 0, j = (int)organism->cells.size(); i < j; i++)
   {
      cell = organism->cells[i];
      addCell(cell);
   }
   organism->cells.clear();
   delete organism;
}
